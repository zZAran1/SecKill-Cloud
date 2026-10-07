package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.client.ProductClient;
import com.example.client.dto.ProductDTO;
import com.example.converter.OrderConverter;
import com.example.exception.BaseBusinessException;
import com.example.mapper.OrderMapper;
import com.example.model.dto.CreateOrderDTO;
import com.example.model.entity.Order;
import com.example.model.vo.OrderVO;
import com.example.result.ErrorCode;
import com.example.result.Result;
import com.example.security.UserContext;
import com.example.service.OrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final ProductClient productClient;

    public OrderServiceImpl(ProductClient productClient) {
        this.productClient = productClient;
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
        order.setStatus(0);
        this.save(order);

        return OrderConverter.INSTANCE.toVO(order);
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long random = ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
        return date + random;
    }

    @Override
    public List<OrderVO> listMyOrders() {
        Long userId = UserContext.getUserId();
        List<Order> orders = this.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime));
        return orders.stream().map(OrderConverter.INSTANCE::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderNo) {
        Long userId = UserContext.getUserId();

        Order order = this.getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BaseBusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BaseBusinessException(ErrorCode.FORBIDDEN);
        }
        // 幂等：支付平台可能重试回调，已支付直接返回成功
        if (order.getStatus() == 1) {
            return;
        }
        if (order.getStatus() != 0) {
            throw new BaseBusinessException(ErrorCode.ORDER_CREATE_FAILED, "订单状态不允许支付");
        }
        // 条件更新：WHERE status=0 保证并发下只有一个请求能流转状态
        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, 0)
                .set(Order::getStatus, 1));
        if (!updated) {
            throw new BaseBusinessException(ErrorCode.ORDER_CREATE_FAILED, "订单状态已变更，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createSeckillOrder(Long userId, Long productId, java.math.BigDecimal seckillPrice, Long activityId) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(productId);
        order.setActivityId(activityId);
        order.setQuantity(1);
        order.setTotalAmount(seckillPrice);
        order.setStatus(0);
        try{
            this.save(order);
        }catch (DuplicateKeyException e){
            throw new BaseBusinessException(ErrorCode.SECKILL_REPEAT);
        }
        return OrderConverter.INSTANCE.toVO(order);
    }
}