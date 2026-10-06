package com.example.service;

import com.example.model.dto.CreateOrderDTO;
import com.example.model.vo.OrderVO;

public interface OrderService {
    OrderVO createOrder(CreateOrderDTO dto);
}