package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.client.ProductClientGuard;
import com.example.client.dto.ProductDTO;
import com.example.config.OrderDelayConfig;
import com.example.constant.SeckillRedisKeys;
import com.example.converter.OrderConverter;
import com.example.exception.BaseBusinessException;
import com.example.mapper.OrderMapper;
import com.example.message.OrderCloseMessage;
import com.example.model.OrderStateMachine;
import com.example.model.OrderStatus;
import com.example.model.dto.CreateOrderDTO;
import com.example.model.entity.Order;
import com.example.model.vo.OrderVO;
import com.example.result.ErrorCode;
import com.example.result.PageResult;
import com.example.result.Result;
import com.example.security.UserContext;
import com.example.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    /**支付超时时间，与 OrderDelayConfig 延迟队列 TTL 共用 order.pay-timeout-minutes 配置*/
    private final long payTimeoutMinutes;

    private final ProductClientGuard productClient;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    public OrderServiceImpl(ProductClientGuard productClient,
                            RabbitTemplate rabbitTemplate,
                            StringRedisTemplate redisTemplate,
                            @Value("${order.pay-timeout-minutes:15}") long payTimeoutMinutes) {
        this.productClient = productClient;
        this.rabbitTemplate = rabbitTemplate;
        this.redisTemplate = redisTemplate;
        this.payTimeoutMinutes = payTimeoutMinutes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderDTO dto) {
        // 1. 网关注入的身份（无 X-User-Id 头时 UserContext 自动抛 401）
        Long userId = UserContext.getUserId();

        // 2. Feign 调商品服务取真实价格，绝不信任客户端传价
        Result<ProductDTO> result = productClient.getProduct(dto.getProductId());
        ProductDTO product = (result == null) ? null : result.getData();
        if (result == null || result.getCode() == null || result.getCode() != 200 || product == null) {
            throw new BaseBusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BaseBusinessException(ErrorCode.ORDER_CREATE_FAILED, "商品已下架");
        }

        // 2.5 扣减库存
        Result<Void> deductResult = productClient.deductStock(dto.getProductId(), dto.getQuantity());
        if (deductResult == null || deductResult.getCode() == null || deductResult.getCode() != 200) {
            throw new BaseBusinessException(ErrorCode.ORDER_CREATE_FAILED, "库存不足");
        }

        // 3. 组装订单落库（0 = 待支付；自增 id 由 MyBatis-Plus 回填）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setActivityId(null);
        order.setQuantity(dto.getQuantity());
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
        order.setStatus(OrderStatus.UNPAID);
        order.setExpireTime(LocalDateTime.now().plusMinutes(payTimeoutMinutes));
        this.save(order);

        // 4. 发延迟消息，支付超时后自动关闭订单并回补库存
        sendDelayCloseMessage(order.getOrderNo());
        return OrderConverter.INSTANCE.toVO(order);
    }

    @Override
    public OrderVO getOrder(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BaseBusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!isAdmin() && !order.getUserId().equals(UserContext.getUserId())) {
            throw new BaseBusinessException(ErrorCode.FORBIDDEN);
        }
        return OrderConverter.INSTANCE.toVO(order);
    }

    @Override
    public PageResult<OrderVO> pageMyOrders(Integer status, long page, long size) {
        Long userId = UserContext.getUserId();
        Page<Order> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(status != null, Order::getStatus, status)
                        .orderByDesc(Order::getCreateTime));
        return toPageResult(result, page, size);
    }

    @Override
    public PageResult<OrderVO> pageOrders(String orderNo, Long userId, Integer status, long page, long size) {
        Page<Order> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(StringUtils.hasText(orderNo), Order::getOrderNo, orderNo)
                        .eq(userId != null, Order::getUserId, userId)
                        .eq(status != null, Order::getStatus, status)
                        .orderByDesc(Order::getCreateTime));
        return toPageResult(result, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(String orderNo) {
        // 状态机校验：待支付→已支付是唯一合法的支付落账路径
        OrderStateMachine.verify(OrderStatus.UNPAID, OrderStatus.PAID);
        // 条件更新：WHERE status=0，与取消/超时关单竞争时只有一个能流转成功
        return this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, OrderStatus.UNPAID)
                .set(Order::getStatus, OrderStatus.PAID)
                .set(Order::getPayTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BaseBusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!isAdmin() && !order.getUserId().equals(UserContext.getUserId())) {
            throw new BaseBusinessException(ErrorCode.FORBIDDEN);
        }
        // 状态机校验：只有已支付订单能确认完成
        OrderStateMachine.verify(order.getStatus(), OrderStatus.COMPLETED);
        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, OrderStatus.PAID)
                .set(Order::getStatus, OrderStatus.COMPLETED));
        if (!updated) {
            throw new BaseBusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态已变更，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toRefunding(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BaseBusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        // 状态机校验：只有已支付/已完成订单能进入退款流程
        OrderStateMachine.verify(order.getStatus(), OrderStatus.REFUNDING);
        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .in(Order::getStatus, OrderStatus.PAID, OrderStatus.COMPLETED)
                .set(Order::getStatus, OrderStatus.REFUNDING));
        if (!updated) {
            throw new BaseBusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态已变更，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toRefunded(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BaseBusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        // 状态机校验：退款中→已退款
        OrderStateMachine.verify(order.getStatus(), OrderStatus.REFUNDED);
        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, OrderStatus.REFUNDING)
                .set(Order::getStatus, OrderStatus.REFUNDED));
        if (!updated) {
            throw new BaseBusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态已变更，请刷新后重试");
        }
        // 退款成功商品回到商家库存，与取消订单共用回补逻辑
        restoreStock(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void backToPaid(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BaseBusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        // 状态机校验：退款中→已支付（退款被驳回/用户撤回申请）
        OrderStateMachine.verify(order.getStatus(), OrderStatus.PAID);
        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, OrderStatus.REFUNDING)
                .set(Order::getStatus, OrderStatus.PAID));
        if (!updated) {
            throw new BaseBusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态已变更，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BaseBusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!isAdmin() && !order.getUserId().equals(UserContext.getUserId())) {
            throw new BaseBusinessException(ErrorCode.FORBIDDEN);
        }
        // 幂等：已取消的订单重复取消直接返回
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        // 状态流转统一在 closeAndRestore 做状态机校验，已支付的订单会被拒绝
        closeAndRestore(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTimeoutOrder(String orderNo) {
        Order order = getByOrderNo(orderNo);
        // 订单不存在 / 已支付 / 已取消：说明消息滞后或订单已被处理，直接跳过
        if (order == null || order.getStatus() != OrderStatus.UNPAID) {
            log.info("超时关闭跳过 orderNo={}，当前状态无需处理", orderNo);
            return;
        }
        log.info("订单支付超时，自动关闭 orderNo={}", orderNo);
        closeAndRestore(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createSeckillOrder(Long userId, Long productId, BigDecimal seckillPrice, Long activityId) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(productId);
        order.setActivityId(activityId);
        order.setQuantity(1);
        order.setTotalAmount(seckillPrice);
        order.setStatus(OrderStatus.UNPAID);
        order.setExpireTime(LocalDateTime.now().plusMinutes(payTimeoutMinutes));
        try{
            this.save(order);
        }catch (DuplicateKeyException e){
            throw new BaseBusinessException(ErrorCode.SECKILL_REPEAT);
        }
        // 秒杀单同样要超时关闭，否则未支付的秒杀库存会被永久占死
        sendDelayCloseMessage(order.getOrderNo());
        return OrderConverter.INSTANCE.toVO(order);
    }

    // ---------- 私有辅助 ----------

    /**
     * 条件更新把订单从待支付流转到已取消，成功后回补库存。
     * 并发下（用户支付 vs 超时关闭）只有一个能更新成功，避免库存被重复回补。
     */
    private void closeAndRestore(Order order) {
        // 状态机校验：只有待支付订单允许变更为已取消
        OrderStateMachine.verify(order.getStatus(), OrderStatus.CANCELLED);
        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, order.getOrderNo())
                .eq(Order::getStatus, OrderStatus.UNPAID)
                .set(Order::getStatus, OrderStatus.CANCELLED));
        if (!updated) {
            log.info("订单关闭竞争失败，已由其他流程处理 orderNo={}", order.getOrderNo());
            return;
        }
        restoreStock(order);
    }

    private void restoreStock(Order order) {
        if (order.getActivityId() != null) {
            // 秒杀单：库存在 Redis，回补库存的同时解除该用户的限购标记
            redisTemplate.execute(SeckillRedisKeys.RESTORE_SCRIPT,
                    List.of(SeckillRedisKeys.STOCK_KEY + order.getActivityId(),
                            SeckillRedisKeys.USERS_KEY + order.getActivityId()),
                    String.valueOf(order.getUserId()));
        } else {
            // 普通单：库存在商品库，通过内部接口回补
            Result<Void> result = productClient.restoreStock(order.getProductId(), order.getQuantity());
            if (result == null || result.getCode() == null || result.getCode() != 200) {
                // 回补失败必须抛出回滚，否则订单关了库存却没回来
                throw new BaseBusinessException(ErrorCode.SYSTEM_ERROR, "库存回补失败，订单关闭已回滚");
            }
        }
        log.info("订单关闭并回补库存 orderNo={}, activityId={}, productId={}, quantity={}",
                order.getOrderNo(), order.getActivityId(), order.getProductId(), order.getQuantity());
    }

    /**
     * 延迟消息发送失败只记录告警，不影响下单结果：
     * 订单已落库是既成事实，回滚反而会造成订单与库存不一致，超时关闭可人工补偿。
     */
    private void sendDelayCloseMessage(String orderNo) {
        try {
            rabbitTemplate.convertAndSend(
                    OrderDelayConfig.ORDER_DELAY_EXCHANGE,
                    OrderDelayConfig.ORDER_DELAY_ROUTING_KEY,
                    new OrderCloseMessage(orderNo));
        } catch (Exception e) {
            log.error("【超时关闭消息发送失败】orderNo={}，该订单需人工关闭", orderNo, e);
        }
    }

    private Order getByOrderNo(String orderNo) {
        return this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    private boolean isAdmin() {
        String role = UserContext.getRole();
        return "0".equals(role) || "1".equals(role);
    }

    private PageResult<OrderVO> toPageResult(Page<Order> result, long page, long size) {
        return PageResult.of(
                result.getRecords().stream().map(OrderConverter.INSTANCE::toVO).toList(),
                result.getTotal(), page, size);
    }

    /**订单号：毫秒时间戳 + 3 位随机数，比「日期 + 随机数」更难预测（超高并发场景应换雪花算法）*/
    private String generateOrderNo() {
        return System.currentTimeMillis()
                + String.valueOf(ThreadLocalRandom.current().nextInt(100, 1000));
    }
}
