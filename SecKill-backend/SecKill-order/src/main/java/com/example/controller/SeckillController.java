package com.example.controller;

import com.example.model.vo.SeckillResultVO;
import com.example.result.Result;
import com.example.service.SeckillService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**秒杀下单：异步建单，结果通过 /seckill/{activityId}/result 轮询*/
    @PostMapping("/{activityId}")
    public Result<Void> seckill(@PathVariable Long activityId) {
        seckillService.seckill(activityId);
        return Result.success("抢购排队中，请稍后查看订单");
    }

    /**查询秒杀结果*/
    @GetMapping("/{activityId}/result")
    public Result<SeckillResultVO> seckillResult(@PathVariable Long activityId) {
        return Result.success(seckillService.getResult(activityId));
    }
}
