package com.example.controller;

import com.example.annotation.RequiresRole;
import com.example.model.dto.CreateRefundDTO;
import com.example.model.vo.RefundVO;
import com.example.result.PageResult;
import com.example.result.Result;
import com.example.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /**用户提交退款申请（已支付/已完成订单可申请，订单进入退款中）*/
    @PostMapping
    public Result<RefundVO> apply(@RequestBody @Valid CreateRefundDTO dto) {
        return Result.success("退款申请已提交，请等待审核", refundService.apply(dto));
    }

    /**用户撤回待审核的退款申请，订单恢复已支付*/
    @PostMapping("/orders/{orderNo}/cancel")
    public Result<Void> cancelByOrder(@PathVariable String orderNo) {
        refundService.cancelByOrder(orderNo);
        return Result.success("退款申请已撤回");
    }

    /**我的退款单（分页）*/
    @GetMapping("/my")
    public Result<PageResult<RefundVO>> pageMyRefunds(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(refundService.pageMyRefunds(status, page, size));
    }

    /**管理员：条件分页查询退款单*/
    @GetMapping("/page")
    @RequiresRole({"0", "1"})
    public Result<PageResult<RefundVO>> pageRefunds(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(refundService.pageRefunds(orderNo, userId, status, page, size));
    }

    /**退款单详情（本人或管理员）*/
    @GetMapping("/{refundNo}")
    public Result<RefundVO> getRefund(@PathVariable String refundNo) {
        return Result.success(refundService.getRefund(refundNo));
    }

    /**管理员审核通过：渠道退款→订单转已退款→回补库存*/
    @PostMapping("/{refundNo}/approve")
    @RequiresRole({"0", "1"})
    public Result<RefundVO> approve(@PathVariable String refundNo) {
        return Result.success("退款成功", refundService.approve(refundNo));
    }

    /**管理员拒绝退款，订单恢复已支付*/
    @PostMapping("/{refundNo}/reject")
    @RequiresRole({"0", "1"})
    public Result<RefundVO> reject(@PathVariable String refundNo,
                                   @RequestParam(required = false) String remark) {
        return Result.success("已拒绝该退款申请", refundService.reject(refundNo, remark));
    }
}
