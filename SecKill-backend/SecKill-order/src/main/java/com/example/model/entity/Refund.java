package com.example.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**退款单：一次退款申请一条记录，全额退款*/
@Data
@TableName("refunds")
public class Refund {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String refundNo;
    private String orderNo;
    private String paymentNo;
    private Long userId;
    private BigDecimal amount;
    /**退款原因*/
    private String reason;
    private Integer status;
    /**审核管理员用户 ID*/
    private Long auditBy;
    /**审核备注（拒绝原因等）*/
    private String auditRemark;
    /**渠道退款流水号*/
    private String channelRefundNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
