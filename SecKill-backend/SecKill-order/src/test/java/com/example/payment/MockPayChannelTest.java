package com.example.payment;

import com.example.model.dto.PayNotifyDTO;
import com.example.model.entity.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockPayChannelTest {

    private final MockPayChannel channel = new MockPayChannel("test-pay-secret");

    private Payment buildPayment() {
        Payment payment = new Payment();
        payment.setPaymentNo("P1730000000000123");
        payment.setOrderNo("1730000000000123");
        payment.setUserId(1L);
        payment.setAmount(new BigDecimal("99.90"));
        return payment;
    }

    @Test
    void 支付回调_本渠道签名_验签通过() {
        PayNotifyDTO dto = channel.pay(buildPayment());
        assertTrue(channel.verifyNotify(dto));
    }

    @Test
    void 篡改回调金额_验签失败() {
        PayNotifyDTO dto = channel.pay(buildPayment());
        dto.setAmount(new BigDecimal("0.01"));
        assertFalse(channel.verifyNotify(dto));
    }

    @Test
    void 篡改回调签名_验签失败() {
        PayNotifyDTO dto = channel.pay(buildPayment());
        dto.setSign("ZmFrZS1zaWduYXR1cmU=");
        assertFalse(channel.verifyNotify(dto));
    }

    @Test
    void 换密钥伪造签名_验签失败() {
        MockPayChannel attacker = new MockPayChannel("wrong-secret");
        PayNotifyDTO dto = attacker.pay(buildPayment());
        assertFalse(channel.verifyNotify(dto));
    }

    @Test
    void 渠道退款_返回退款流水号() {
        String channelRefundNo = channel.refund(buildPayment());
        assertNotNull(channelRefundNo);
        assertTrue(channelRefundNo.startsWith("MOCKR"));
    }
}
