package com.example.client;

import com.example.client.dto.ProductDTO;
import com.example.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "seckill-product")
public interface ProductClient {

    @GetMapping("/internal/products/{id}")
    Result<ProductDTO> getProduct(@PathVariable("id") Long id);

    @PostMapping("/internal/products/{id}/deduct-stock")
    Result<Void> deductStock(@PathVariable("id") Long id, @RequestParam Integer quantity);

}