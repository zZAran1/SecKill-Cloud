package com.example.converter;

import com.example.model.dto.ProductsDTO;
import com.example.model.entity.Products;
import com.example.model.vo.ProductsVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProductConverter {
    ProductConverter INSTANCE = Mappers.getMapper(ProductConverter.class);
    Products toEntity(ProductsDTO dto);
    ProductsVO toVO(Products products);
}
