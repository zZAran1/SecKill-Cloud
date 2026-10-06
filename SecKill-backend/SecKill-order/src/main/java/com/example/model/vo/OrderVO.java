package com.example.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderVO {
    private String orderNo;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer status;
}