package com.example.payment;

import com.example.model.dto.PayNotifyDTO;
import com.example.model.entity.Payment;

/**
 * 支付渠道抽象：当前只有模拟渠道 {@link MockPayChannel}。
 * 接入真实第三方（支付宝/微信）时新增实现类替换 Bean 即可，
 * PaymentService 只依赖本接口，业务代码无需改动。
 */
public interface PayChannel {

    /**渠道标识，落库到 payments.channel*/
    String channel();

    /**
     * 发起渠道支付。
     * 模拟渠道：立即"支付成功"并返回带签名的回调报文；
     * 真实渠道：应返回收银台跳转参数，由前端拉起支付，支付结果走异步回调。
     */
    PayNotifyDTO pay(Payment payment);

    /**校验渠道回调签名，防止伪造回调篡改支付结果*/
    boolean verifyNotify(PayNotifyDTO dto);

    /**
     * 渠道退款。
     * 模拟渠道：立即成功；真实渠道失败会抛异常，由调用方回滚本地状态，
     * 极端情况（渠道成功但本地落账失败）需人工对账补偿。
     *
     * @return 渠道退款流水号
     */
    String refund(Payment payment);
}
