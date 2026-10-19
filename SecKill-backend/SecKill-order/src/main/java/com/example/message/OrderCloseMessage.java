package com.example.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单超时关闭消息，只带订单号，消费端查库拿完整订单再决定是否关闭
 */
@Data
public class OrderCloseMessage implements Serializable {

    private String orderNo;

    public OrderCloseMessage() {
    }

    public OrderCloseMessage(String orderNo) {
        this.orderNo = orderNo;
    }
}
