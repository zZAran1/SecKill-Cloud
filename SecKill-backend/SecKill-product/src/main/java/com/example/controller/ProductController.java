package com.example.controller;

import com.example.annotation.RequiresRole;
import com.example.model.dto.ProductStatusDTO;
import com.example.model.dto.ProductsDTO;
import com.example.model.dto.UpdateProductDTO;
import com.example.model.vo.ProductsVO;
import com.example.result.PageResult;
import com.example.result.Result;
import com.example.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @RequiresRole({"0","1"})
    public Result<Void> addProduct(@RequestBody @Valid ProductsDTO dto) {
        productService.addProduct(dto);
        return Result.success("添加商品成功");
    }

    /**编辑商品*/
    @PutMapping("/{id}")
    @RequiresRole({"0","1"})
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody @Valid UpdateProductDTO dto) {
        productService.updateProduct(id, dto);
        return Result.success("商品更新成功");
    }

    /**上下架*/
    @PutMapping("/{id}/status")
    @RequiresRole({"0","1"})
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody @Valid ProductStatusDTO dto) {
        productService.updateStatus(id, dto);
        return Result.success("商品状态已更新");
    }

    /**分页查询（status 不传只返回上架商品，管理端传 0/1 查看下架或上架）*/
    @GetMapping("/page")
    public Result<PageResult<ProductsVO>> pageProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(productService.pageProducts(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<ProductsVO> getProduct(@PathVariable Long id) {
        return Result.success(productService.getProduct(id));
    }

    @GetMapping
    public Result<List<ProductsVO>> listProducts(@RequestParam(required = false) String keyword) {
        return Result.success(productService.listProducts(keyword));
    }

}
