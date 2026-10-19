package com.example.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品上下架：0 下架，1 上架
 */
@Data
public class ProductStatusDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
