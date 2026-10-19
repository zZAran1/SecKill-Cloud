package com.example.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动修改（不允许改关联商品，仅未开始的活动可修改）
 */
@Data
public class UpdateSeckillActivityDTO {

    @NotNull(message = "秒杀价格不能为空")
    @DecimalMin(value = "0.01", message = "价格不能低于0.01")
    private BigDecimal seckillPrice;

    @NotNull(message = "秒杀库存不能为空")
    @Min(value = 1, message = "秒杀库存不能低于1")
    private Integer stock;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
