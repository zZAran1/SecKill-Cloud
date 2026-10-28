package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.model.dto.CreateOrderDTO;
import com.example.model.entity.Order;
import com.example.model.vo.OrderVO;
import com.example.result.PageResult;

import java.math.BigDecimal;

public interface OrderService extends IService<Order> {

    OrderVO createOrder(CreateOrderDTO dto);

    /**订单详情（本人或管理员）*/
    OrderVO getOrder(String orderNo);

    /**我的订单（分页），status 不传查全部*/
    PageResult<OrderVO> pageMyOrders(Integer status, long page, long size);

    /**
     * 支付成功后把订单从待支付流转到已支付（条件更新，防与取消/超时关单并发）。
     * 只允许 PaymentService 的回调落账逻辑调用。
     *
     * @return true 流转成功；false 订单已不是待支付（已被取消/关闭）
     */
    boolean markPaid(String orderNo);

    /**确认完成：已支付→已完成（本人或管理员）*/
    void completeOrder(String orderNo);

    /**退款申请受理：已支付/已完成→退款中（仅 RefundService 调用）*/
    void toRefunding(String orderNo);

    /**退款成功落账：退款中→已退款，并回补库存（仅 RefundService 调用）*/
    void toRefunded(String orderNo);

    /**退款驳回/撤回：退款中→已支付（仅 RefundService 调用）*/
    void backToPaid(String orderNo);

    /**取消待支付订单，成功后回补库存*/
    void cancelOrder(String orderNo);

    /**管理员：条件分页查询订单*/
    PageResult<OrderVO> pageOrders(String orderNo, Long userId, Integer status, long page, long size);

    OrderVO createSeckillOrder(Long userId, Long productId, BigDecimal seckillPrice, Long activityId);

    /**延迟消息触发的超时关闭：只有仍处于待支付的订单才会被关闭*/
    void closeTimeoutOrder(String orderNo);
}
