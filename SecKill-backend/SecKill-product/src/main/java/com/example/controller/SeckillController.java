package com.example.controller;

import com.example.annotation.RequiresRole;
import com.example.result.Result;
import com.example.service.SeckillActivityService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill-activities")
public class SeckillController {

    private final SeckillActivityService seckillActivityService;

    public SeckillController(SeckillActivityService seckillActivityService) {
        this.seckillActivityService = seckillActivityService;
    }

    @PostMapping("/{id}/preload")
    @RequiresRole({"0","1"})
    public Result<Void> preload(@PathVariable Long id) {
        seckillActivityService.preloadActivity(id);
        return Result.success("预热成功");
    }
}