package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.model.dto.PayNotifyDTO;
import com.example.model.entity.Payment;
import com.example.model.vo.PaymentVO;

public interface PaymentService extends IService<Payment> {

    /**
     * 发起支付：创建（或复用）支付单并调用支付渠道。
     * 模拟渠道即时回调，接口返回时订单已流转为已支付。
     */
    PaymentVO payOrder(String orderNo);

    /**支付渠道异步回调：验签 + 幂等落账 + 订单状态机流转，真实渠道回调也走这里*/
    PaymentVO handleNotify(PayNotifyDTO dto);

    /**
     * 渠道退款：支付单已支付→已退款（条件更新防并发重复退款），再调渠道退款。
     *
     * @return 渠道退款流水号
     */
    String refund(String orderNo);

    /**查询支付单（本人或管理员）*/
    PaymentVO getPayment(String paymentNo);

    /**按订单号查支付单，不存在抛 PAYMENT_NOT_FOUND*/
    PaymentVO getByOrder(String orderNo);
}
