package com.example.controller;

import com.example.model.vo.ProductsVO;
import com.example.result.Result;
import com.example.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/products")
public class InternalProductController {

    private final ProductService productService;

    public InternalProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/{id}/deduct-stock")
    public Result<Void> deductStock(@PathVariable Long id, @RequestParam Integer quantity) {
        productService.deductStock(id, quantity);
        return Result.success("扣减库存成功");
    }

    @GetMapping("{id}")
    public Result<ProductsVO>  getProducts(@PathVariable Long id) {
        return Result.success(productService.getProduct(id));
    }
}