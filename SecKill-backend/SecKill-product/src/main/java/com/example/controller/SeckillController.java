package com.example.controller;

import com.example.annotation.RequiresRole;
import com.example.model.dto.SeckillActivityDTO;
import com.example.model.dto.UpdateSeckillActivityDTO;
import com.example.model.vo.SeckillActivityVO;
import com.example.result.PageResult;
import com.example.result.Result;
import com.example.service.SeckillActivityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seckill-activities")
public class SeckillController {

    private final SeckillActivityService seckillActivityService;

    public SeckillController(SeckillActivityService seckillActivityService) {
        this.seckillActivityService = seckillActivityService;
    }

    /**创建秒杀活动*/
    @PostMapping
    @RequiresRole({"0","1"})
    public Result<SeckillActivityVO> createActivity(@RequestBody @Valid SeckillActivityDTO dto) {
        return Result.success("活动创建成功", seckillActivityService.createActivity(dto));
    }

    /**修改秒杀活动（仅未开始的活动可修改）*/
    @PutMapping("/{id}")
    @RequiresRole({"0","1"})
    public Result<SeckillActivityVO> updateActivity(@PathVariable Long id,
                                                    @RequestBody @Valid UpdateSeckillActivityDTO dto) {
        return Result.success("活动更新成功", seckillActivityService.updateActivity(id, dto));
    }

    /**预热：活动信息与库存写入 Redis*/
    @PostMapping("/{id}/preload")
    @RequiresRole({"0","1"})
    public Result<Void> preload(@PathVariable Long id) {
        seckillActivityService.preloadActivity(id);
        return Result.success("预热成功");
    }

    /**上线*/
    @PutMapping("/{id}/online")
    @RequiresRole({"0","1"})
    public Result<Void> online(@PathVariable Long id) {
        seckillActivityService.online(id);
        return Result.success("活动已上线");
    }

    /**下线*/
    @PutMapping("/{id}/offline")
    @RequiresRole({"0","1"})
    public Result<Void> offline(@PathVariable Long id) {
        seckillActivityService.offline(id);
        return Result.success("活动已下线");
    }

    /**活动详情（含实时剩余库存）*/
    @GetMapping("/{id}")
    public Result<SeckillActivityVO> getActivity(@PathVariable Long id) {
        return Result.success(seckillActivityService.getActivity(id));
    }

    /**分页查询活动*/
    @GetMapping("/page")
    public Result<PageResult<SeckillActivityVO>> pageActivities(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(seckillActivityService.pageActivities(status, page, size));
    }
}
