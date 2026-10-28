package com.example.controller;

import com.example.model.dto.PayNotifyDTO;
import com.example.model.vo.PaymentVO;
import com.example.result.Result;
import com.example.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 支付渠道异步回调（公开接口，网关白名单放行）。
     * 安全不依赖登录态，完全靠回调报文签名验证（payment.secret），
     * 接入支付宝/微信时把各自回调参数解析成 PayNotifyDTO 后走同一处理逻辑。
     */
    @PostMapping("/notify")
    public Result<PaymentVO> payNotify(@RequestBody @Valid PayNotifyDTO dto) {
        return Result.success("支付成功", paymentService.handleNotify(dto));
    }

    /**查询支付单（本人或管理员）*/
    @GetMapping("/{paymentNo}")
    public Result<PaymentVO> getPayment(@PathVariable String paymentNo) {
        return Result.success(paymentService.getPayment(paymentNo));
    }
}
