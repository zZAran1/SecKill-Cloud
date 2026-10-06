package com.example.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivityDTO {
    @NotNull(message = "商品id不能为空")
    private Long productId;

    @NotNull(message = "秒杀价格不能为空")
    @DecimalMin(value = "0.01",message = "价格表不能低于0.01")
    private BigDecimal seckillPrice;

    @NotNull(message = "商品库存不能为空")
    @Min(value = 1,message = "商品库存不能低于1")
    private Integer stock;

    @NotNull(message = "开始时间不能为空")
    @Future
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
