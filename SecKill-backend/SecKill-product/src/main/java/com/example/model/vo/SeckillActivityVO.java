package com.example.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivityVO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal seckillPrice;
    private BigDecimal originalPrice;
    /**秒杀专用库存*/
    private Integer stock;
    /**实时剩余库存（Redis 未预热时回落到 DB 库存）*/
    private Integer remainingStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /**0 下线，1 上线*/
    private Integer status;
    private LocalDateTime createTime;
}
