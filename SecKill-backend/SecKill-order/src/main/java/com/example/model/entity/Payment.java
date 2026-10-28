package com.example.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**支付流水：一笔订单对应一条支付单，记录渠道、金额、第三方流水号*/
@Data
@TableName("payments")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paymentNo;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    /**支付渠道 MOCK/ALIPAY/WXPAY*/
    private String channel;
    private Integer status;
    /**第三方支付流水号*/
    private String tradeNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
