package com.example.model.vo;

import lombok.Data;

/**
 * 秒杀结果查询：秒杀下单是异步的，前端轮询该接口拿到最终结果
 */
@Data
public class SeckillResultVO {

    /**0 排队中，1 抢购成功，2 未抢到*/
    private Integer result;

    private String message;

    /**抢购成功时返回*/
    private String orderNo;

    /**抢购成功时返回订单状态*/
    private Integer orderStatus;

    public static SeckillResultVO queuing() {
        SeckillResultVO vo = new SeckillResultVO();
        vo.result = 0;
        vo.message = "抢购排队中，请稍后查看";
        return vo;
    }

    public static SeckillResultVO success(String orderNo, Integer orderStatus) {
        SeckillResultVO vo = new SeckillResultVO();
        vo.result = 1;
        vo.message = "抢购成功，请尽快支付";
        vo.orderNo = orderNo;
        vo.orderStatus = orderStatus;
        return vo;
    }

    public static SeckillResultVO failed() {
        SeckillResultVO vo = new SeckillResultVO();
        vo.result = 2;
        vo.message = "未抢到，手慢了";
        return vo;
    }
}
