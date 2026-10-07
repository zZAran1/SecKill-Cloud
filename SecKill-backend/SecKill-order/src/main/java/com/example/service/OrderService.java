package com.example.service;

import com.example.model.dto.CreateOrderDTO;
import com.example.model.vo.OrderVO;

import java.util.List;

public interface OrderService {
    OrderVO createOrder(CreateOrderDTO dto);
    List<OrderVO> listMyOrders();
    void payOrder(String orderNo);
    OrderVO createSeckillOrder(Long userId, Long productId, java.math.BigDecimal seckillPrice, Long activityId);
}