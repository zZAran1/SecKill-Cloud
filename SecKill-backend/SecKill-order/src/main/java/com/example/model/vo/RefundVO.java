package com.example.model.vo;

import com.example.model.RefundStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundVO {
    private String refundNo;
    private String orderNo;
    private String paymentNo;
    private Long userId;
    /**退款金额（全额退款）*/
    private BigDecimal amount;
    private String reason;
    /**0待审核 1退款成功 2已拒绝 3已取消*/
    private Integer status;
    /**审核管理员用户 ID*/
    private Long auditBy;
    private String auditRemark;
    /**渠道退款流水号*/
    private String channelRefundNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**状态描述，由 status 推导*/
    public String getStatusDesc() {
        return status == null ? null : RefundStatus.desc(status);
    }
}
