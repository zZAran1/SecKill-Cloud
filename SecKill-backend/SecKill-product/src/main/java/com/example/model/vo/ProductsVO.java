package com.example.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class ProductsVO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer status;
    private Integer stock;
    private String imageUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
