package com.example.controller;

import com.example.annotation.RequiresRole;
import com.example.model.dto.CreateOrderDTO;
import com.example.model.vo.OrderVO;
import com.example.result.PageResult;
import com.example.result.Result;
import com.example.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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

    /**我的订单（分页）*/
    @GetMapping("/my")
    public Result<PageResult<OrderVO>> pageMyOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(orderService.pageMyOrders(status, page, size));
    }

    /**管理员：条件分页查询订单*/
    @GetMapping("/page")
    @RequiresRole({"0","1"})
    public Result<PageResult<OrderVO>> pageOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(orderService.pageOrders(orderNo, userId, status, page, size));
    }

    /**订单详情（本人或管理员）*/
    @GetMapping("/{orderNo}")
    public Result<OrderVO> getOrder(@PathVariable String orderNo) {
        return Result.success(orderService.getOrder(orderNo));
    }

    @PostMapping("/{orderNo}/pay")
    public Result<Void> payOrder(@PathVariable String orderNo) {
        orderService.payOrder(orderNo);
        return Result.success("支付成功");
    }

    /**取消待支付订单，成功后回补库存*/
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        orderService.cancelOrder(orderNo);
        return Result.success("订单已取消");
    }
}
