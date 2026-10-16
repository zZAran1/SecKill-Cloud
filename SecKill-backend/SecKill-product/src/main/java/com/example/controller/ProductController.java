package com.example.controller;

import com.example.annotation.RequiresRole;
import com.example.model.dto.ProductsDTO;
import com.example.model.vo.ProductsVO;
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

    @GetMapping("/{id}")
    public Result<ProductsVO> getProduct(@PathVariable Long id) {
        return Result.success(productService.getProduct(id));
    }

    @GetMapping
    public Result<List<ProductsVO>> listProducts(@RequestParam(required = false) String keyword) {
        return Result.success(productService.listProducts(keyword));
    }

}
