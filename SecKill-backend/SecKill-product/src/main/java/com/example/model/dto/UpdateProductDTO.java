package com.example.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品编辑：字段可选，只更新传入的字段。
 * 库存通常由扣减/回补接口维护，这里仅用于盘点时人工调整。
 */
@Data
public class UpdateProductDTO {

    @Size(max = 100, message = "商品名称不能超过100个字")
    private String name;

    @Size(max = 1000, message = "商品描述不能超过1000个字")
    private String description;

    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;

    @Size(max = 255, message = "图片地址过长")
    private String imageUrl;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;
}
