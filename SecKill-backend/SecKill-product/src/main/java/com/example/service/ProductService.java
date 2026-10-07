package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.model.dto.ProductsDTO;
import com.example.model.entity.Products;
import com.example.model.vo.ProductsVO;

import java.util.List;

public interface ProductService extends IService<Products> {

    void addProduct(ProductsDTO dto);

    ProductsVO getProduct(Long id);

    List<ProductsVO> listProducts(String keyword);

    void deductStock(Long id, Integer quantity);
}
