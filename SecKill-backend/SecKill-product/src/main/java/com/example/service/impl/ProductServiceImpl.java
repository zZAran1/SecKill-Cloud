package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.converter.ProductConverter;
import com.example.exception.ProductException;
import com.example.mapper.ProductMapper;
import com.example.model.dto.ProductsDTO;
import com.example.model.entity.Products;
import com.example.model.vo.ProductsVO;
import com.example.result.ErrorCode;
import com.example.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Products> implements ProductService {

    private static final int STATUS_ON_SHELF = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(ProductsDTO dto) {
        Products product = ProductConverter.INSTANCE.toEntity(dto);
        product.setStatus(STATUS_ON_SHELF);
        this.save(product);
    }

    @Override
    public ProductsVO getProduct(Long id) {
        Products product = this.getById(id);
        if (product == null) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return ProductConverter.INSTANCE.toVO(product);
    }

    @Override
    public List<ProductsVO> listProducts(String keyword) {
        return this.lambdaQuery()
                .like(StringUtils.hasText(keyword), Products::getName, keyword)
                .eq(Products::getStatus, STATUS_ON_SHELF)
                .list()
                .stream()
                .map(ProductConverter.INSTANCE::toVO)
                .toList();
    }


}
