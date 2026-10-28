package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.converter.PaymentConverter;
import com.example.exception.BaseBusinessException;
import com.example.mapper.PaymentMapper;
import com.example.model.OrderStatus;
import com.example.model.PaymentStatus;
import com.example.model.dto.PayNotifyDTO;
import com.example.model.entity.Payment;
import com.example.model.vo.OrderVO;
import com.example.model.vo.PaymentVO;
import com.example.payment.PayChannel;
import com.example.result.ErrorCode;
import com.example.security.UserContext;
import com.example.service.OrderService;
import com.example.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final OrderService orderService;
    private final PayChannel payChannel;

    public PaymentServiceImpl(OrderService orderService, PayChannel payChannel) {
        this.orderService = orderService;
        this.payChannel = payChannel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO payOrder(String orderNo) {
        // 1. 订单必须存在且归属当前用户（getOrder 内部已完成归属/管理员校验）
        OrderVO order = orderService.getOrder(orderNo);

        // 2. 幂等：已成功的支付单直接返回，重复支付不会二次扣款
        Payment payment = getByOrderNo(orderNo);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            return toVO(payment);
        }

        // 3. 只有待支付订单能发起支付
        if (order.getStatus() == null || order.getStatus() != OrderStatus.UNPAID) {
            throw new BaseBusinessException(ErrorCode.ORDER_STATUS_ERROR,
                    "当前订单状态为「" + OrderStatus.desc(order.getStatus()) + "」，不能支付");
        }

        // 4. 创建支付单（一单一条；uk_order_no 唯一索引兜底并发重复创建）
        if (payment == null) {
            payment = new Payment();
            payment.setPaymentNo(generatePaymentNo());
            payment.setOrderNo(orderNo);
            payment.setUserId(order.getUserId());
            payment.setAmount(order.getTotalAmount());
            payment.setChannel(payChannel.channel());
            payment.setStatus(PaymentStatus.UNPAID);
            try {
                this.save(payment);
            } catch (DuplicateKeyException e) {
                payment = getByOrderNo(orderNo);
            }
        }

        // 5. 调用支付渠道并处理回调
        //    模拟渠道即时回调，返回时订单已流转为已支付；
        //    接真实渠道时这里改为返回收银台跳转参数，支付结果由渠道异步回调 handleNotify
        PayNotifyDTO notify = payChannel.pay(payment);
        return handleNotify(notify);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO handleNotify(PayNotifyDTO dto) {
        // 1. 验签：伪造回调直接拒绝
        if (!payChannel.verifyNotify(dto)) {
            throw new BaseBusinessException(ErrorCode.PAYMENT_SIGN_ERROR);
        }
        Payment payment = getByPaymentNo(dto.getPaymentNo());
        if (payment == null) {
            throw new BaseBusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        // 2. 幂等：支付渠道可能重试回调，已成功直接返回
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toVO(payment);
        }
        if (payment.getStatus() != PaymentStatus.UNPAID) {
            throw new BaseBusinessException(ErrorCode.PAYMENT_STATUS_ERROR);
        }
        // 3. 金额校验：防篡改回调金额
        if (dto.getAmount() == null || dto.getAmount().compareTo(payment.getAmount()) != 0) {
            throw new BaseBusinessException(ErrorCode.PAYMENT_SIGN_ERROR, "回调金额与支付单不一致");
        }

        // 4. 条件更新占住支付单（WHERE status=0），并发重复回调只有一个能占住
        boolean claimed = this.update(new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getPaymentNo, payment.getPaymentNo())
                .eq(Payment::getStatus, PaymentStatus.UNPAID)
                .set(Payment::getStatus, PaymentStatus.SUCCESS)
                .set(Payment::getTradeNo, dto.getTradeNo()));
        if (!claimed) {
            // 没占住说明并发回调已处理完，重查返回
            return toVO(getByPaymentNo(dto.getPaymentNo()));
        }

        // 5. 订单状态机流转：待支付→已支付（条件更新防与取消/超时关单并发）
        if (!orderService.markPaid(payment.getOrderNo())) {
            // 订单已被取消/关闭：支付单置失败保证两单状态一致（真实渠道应接着发起退款）
            this.update(new LambdaUpdateWrapper<Payment>()
                    .eq(Payment::getPaymentNo, payment.getPaymentNo())
                    .set(Payment::getStatus, PaymentStatus.FAILED));
            log.error("【支付回调】订单已关闭，支付单置为失败 paymentNo={} orderNo={}，真实渠道需走退款",
                    payment.getPaymentNo(), payment.getOrderNo());
        }
        return toVO(getByPaymentNo(payment.getPaymentNo()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String refund(String orderNo) {
        Payment payment = getByOrderNo(orderNo);
        if (payment == null) {
            throw new BaseBusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        // 条件更新先占住支付单（WHERE status=1），并发重复退款只有一个能占住
        boolean claimed = this.update(new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getPaymentNo, payment.getPaymentNo())
                .eq(Payment::getStatus, PaymentStatus.SUCCESS)
                .set(Payment::getStatus, PaymentStatus.REFUNDED));
        if (!claimed) {
            throw new BaseBusinessException(ErrorCode.PAYMENT_STATUS_ERROR, "该支付单不可退款，请勿重复操作");
        }
        // 调渠道退款（模拟渠道即时成功；真实渠道失败抛异常回滚本地状态）
        return payChannel.refund(payment);
    }

    @Override
    public PaymentVO getPayment(String paymentNo) {
        Payment payment = getByPaymentNo(paymentNo);
        if (payment == null) {
            throw new BaseBusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        String role = UserContext.getRole();
        boolean isAdmin = "0".equals(role) || "1".equals(role);
        if (!isAdmin && !payment.getUserId().equals(UserContext.getUserId())) {
            throw new BaseBusinessException(ErrorCode.FORBIDDEN);
        }
        return toVO(payment);
    }

    @Override
    public PaymentVO getByOrder(String orderNo) {
        Payment payment = getByOrderNo(orderNo);
        if (payment == null) {
            throw new BaseBusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        return toVO(payment);
    }

    // ---------- 私有辅助 ----------

    private Payment getByOrderNo(String orderNo) {
        return this.getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo));
    }

    private Payment getByPaymentNo(String paymentNo) {
        return this.getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentNo, paymentNo));
    }

    private PaymentVO toVO(Payment payment) {
        return PaymentConverter.INSTANCE.toVO(payment);
    }

    /**支付单号：P + 毫秒时间戳 + 3 位随机数，与订单号生成风格一致*/
    private String generatePaymentNo() {
        return "P" + System.currentTimeMillis()
                + String.valueOf(ThreadLocalRandom.current().nextInt(100, 1000));
    }
}
