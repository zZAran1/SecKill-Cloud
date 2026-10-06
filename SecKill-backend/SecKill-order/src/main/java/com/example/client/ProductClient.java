package com.example.client;

import com.example.client.dto.ProductDTO;
import com.example.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "seckill-product")
public interface ProductClient {

    @GetMapping("/products/{id}")
    Result<ProductDTO> getProduct(@PathVariable("id") Long id);
}