package com.example.service;

import com.example.model.vo.SeckillResultVO;

public interface SeckillService {
    void seckill(Long activityId);

    /**查询秒杀结果：排队中 / 成功 / 未抢到*/
    SeckillResultVO getResult(Long activityId);
}
