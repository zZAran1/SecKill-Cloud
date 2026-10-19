package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.model.dto.ProductStatusDTO;
import com.example.model.dto.ProductsDTO;
import com.example.model.dto.UpdateProductDTO;
import com.example.model.entity.Products;
import com.example.model.vo.ProductsVO;
import com.example.result.PageResult;

import java.util.List;

public interface ProductService extends IService<Products> {

    void addProduct(ProductsDTO dto);

    /**编辑商品（只更新传入的字段）*/
    void updateProduct(Long id, UpdateProductDTO dto);

    /**上下架*/
    void updateStatus(Long id, ProductStatusDTO dto);

    ProductsVO getProduct(Long id);

    List<ProductsVO> listProducts(String keyword);

    /**分页查询，status 不传时只返回上架商品，管理端显式传 0/1 查看下架或上架*/
    PageResult<ProductsVO> pageProducts(String keyword, Integer status, long page, long size);

    /**扣减库存（乐观锁，不允许扣成负数）*/
    void deductStock(Long id, Integer quantity);

    /**回补库存（订单取消/超时关闭时调用）*/
    void restoreStock(Long id, Integer quantity);
}
