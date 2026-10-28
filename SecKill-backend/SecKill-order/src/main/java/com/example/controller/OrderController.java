package com.example.controller;

import com.example.annotation.RequiresRole;
import com.example.model.PaymentStatus;
import com.example.model.dto.CreateOrderDTO;
import com.example.model.vo.OrderVO;
import com.example.model.vo.PaymentVO;
import com.example.result.PageResult;
import com.example.result.Result;
import com.example.service.OrderService;
import com.example.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
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

    /**
     * 发起支付：创建支付单并调用支付渠道。
     * 模拟渠道即时完成支付，返回支付单信息；接真实渠道后由前端拉起收银台。
     */
    @PostMapping("/{orderNo}/pay")
    public Result<PaymentVO> payOrder(@PathVariable String orderNo) {
        PaymentVO payment = paymentService.payOrder(orderNo);
        // 极端竞态下（支付瞬间订单被超时关闭）支付单会落为失败，消息要如实反馈
        String msg = payment.getStatus() != null && payment.getStatus() == PaymentStatus.SUCCESS
                ? "支付成功" : "支付未完成，请刷新订单状态";
        return Result.success(msg, payment);
    }

    /**确认完成：已支付→已完成*/
    @PostMapping("/{orderNo}/complete")
    public Result<Void> completeOrder(@PathVariable String orderNo) {
        orderService.completeOrder(orderNo);
        return Result.success("订单已完成");
    }

    /**取消待支付订单，成功后回补库存*/
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        orderService.cancelOrder(orderNo);
        return Result.success("订单已取消");
    }
}
