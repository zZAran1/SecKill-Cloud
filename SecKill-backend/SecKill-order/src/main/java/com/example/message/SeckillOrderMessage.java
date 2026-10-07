package com.example.message;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SeckillOrderMessage implements Serializable {
    private Long userId;
    private Long productId;
    private Long activityId;
    private BigDecimal seckillPrice;
}