package com.example.model.vo;

import com.example.model.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentVO {
    private String paymentNo;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    /**支付渠道 MOCK/ALIPAY/WXPAY*/
    private String channel;
    /**0待支付 1支付成功 2支付失败 3已关闭*/
    private Integer status;
    /**第三方支付流水号*/
    private String tradeNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**状态描述，由 status 推导*/
    public String getStatusDesc() {
        return status == null ? null : PaymentStatus.desc(status);
    }
}
