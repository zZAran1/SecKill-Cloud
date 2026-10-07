package com.example.controller;

import com.example.result.Result;
import com.example.service.SeckillService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/{activityId}")
    public Result<Void> seckill(@PathVariable Long activityId) {
        seckillService.seckill(activityId);
        return Result.success("抢购排队中，请稍后查看订单");
    }
}