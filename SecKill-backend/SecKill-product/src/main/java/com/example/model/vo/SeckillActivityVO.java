package com.example.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class SeckillActivityVO {
    private String productName;
    private String productImage;
    private BigDecimal seckillPrice;
    private BigDecimal originalPrice;
    private Integer activityStatus;
}
