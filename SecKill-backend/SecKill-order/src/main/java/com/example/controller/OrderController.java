package com.example.controller;

import com.example.model.dto.CreateOrderDTO;
import com.example.model.vo.OrderVO;
import com.example.result.Result;
import com.example.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderVO> createOrder(@RequestBody @Valid CreateOrderDTO dto) {
        return Result.success("下单成功", orderService.createOrder(dto));
    }
}