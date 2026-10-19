package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.model.dto.SeckillActivityDTO;
import com.example.model.dto.UpdateSeckillActivityDTO;
import com.example.model.entity.SeckillActivity;
import com.example.model.vo.SeckillActivityVO;
import com.example.result.PageResult;

public interface SeckillActivityService extends IService<SeckillActivity> {

    /**创建秒杀活动*/
    SeckillActivityVO createActivity(SeckillActivityDTO dto);

    /**修改秒杀活动（仅未开始的活动可修改）*/
    SeckillActivityVO updateActivity(Long id, UpdateSeckillActivityDTO dto);

    /**分页查询活动，status 不传时只返回上线活动，管理端显式传 0/1 查看下线或上线*/
    PageResult<SeckillActivityVO> pageActivities(Integer status, long page, long size);

    /**活动详情（含实时剩余库存）*/
    SeckillActivityVO getActivity(Long id);

    /**上线*/
    void online(Long id);

    /**下线（同时清理 Redis 中的活动信息与库存）*/
    void offline(Long id);

    /**预热：活动信息与库存写入 Redis*/
    void preloadActivity(Long id);
}
