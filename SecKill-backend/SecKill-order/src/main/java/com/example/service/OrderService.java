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

    void payOrder(String orderNo);

    /**取消待支付订单，成功后回补库存*/
    void cancelOrder(String orderNo);

    /**管理员：条件分页查询订单*/
    PageResult<OrderVO> pageOrders(String orderNo, Long userId, Integer status, long page, long size);

    OrderVO createSeckillOrder(Long userId, Long productId, BigDecimal seckillPrice, Long activityId);

    /**延迟消息触发的超时关闭：只有仍处于待支付的订单才会被关闭*/
    void closeTimeoutOrder(String orderNo);
}
