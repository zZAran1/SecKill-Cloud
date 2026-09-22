package com.example.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class ProductsDTO {
    @NotBlank(message = "商品名称不能为空")
    @Size(max=100,message = "商品名称不能超过100个字")
    private String name;

    @Size(max=1000,message = "商品描述不能超过1000个字")
    private String description;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01",message = "商品价格必须大于0")
    private BigDecimal price;

    @Size(max=255,message = "图片地址过长")
    private String imageUrl;
}
