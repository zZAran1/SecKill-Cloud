package com.example.controller;

import com.example.model.dto.CreateOrderDTO;
import com.example.model.vo.OrderVO;
import com.example.result.Result;
import com.example.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/my")
    public Result<List<OrderVO>> listMyOrders() {
        return Result.success(orderService.listMyOrders());
    }

    @PostMapping("/{orderNo}/pay")
    public Result<Void> payOrder(@PathVariable String orderNo) {
        orderService.payOrder(orderNo);
        return Result.success("支付成功");
    }
}