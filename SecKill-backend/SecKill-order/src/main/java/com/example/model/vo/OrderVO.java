package com.example.model.vo;

import com.example.model.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {
    private String orderNo;
    private Long userId;
    private Long productId;
    /**秒杀活动 ID，普通订单为 null*/
    private Long activityId;
    private Integer quantity;
    private BigDecimal totalAmount;
    /**0 待支付 1 已支付 2 已取消 3 已完成 4 退款中 5 已退款*/
    private Integer status;
    /**支付成功时间*/
    private LocalDateTime payTime;
    /**支付截止时间*/
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**状态描述，由 status 推导*/
    public String getStatusDesc() {
        return status == null ? null : OrderStatus.desc(status);
    }
}
