package com.example.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付渠道回调报文（模拟渠道与真实渠道通用）。
 * sign 为渠道对 paymentNo+tradeNo+amount 的签名，服务端用 payment.secret 验签，
 * 防止伪造回调把未支付订单改成已支付。
 */
@Data
public class PayNotifyDTO {

    @NotBlank(message = "支付单号不能为空")
    private String paymentNo;

    /**第三方支付流水号*/
    @NotBlank(message = "支付流水号不能为空")
    private String tradeNo;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    @NotBlank(message = "签名不能为空")
    private String sign;
}
