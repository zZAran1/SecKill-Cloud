package com.example.payment;

import com.example.model.dto.PayNotifyDTO;
import com.example.model.entity.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟支付渠道：生成回调报文并用 payment.secret 签名，回调验签用同一密钥，
 * 与真实渠道「回调必须验签」的安全模型保持一致。
 *
 * 签名算法：HmacSHA256，数据格式 paymentNo={}&tradeNo={}&amount={}，结果 Base64（与网关签名风格统一）。
 */
@Component
public class MockPayChannel implements PayChannel {

    public static final String CHANNEL_MOCK = "MOCK";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";

    private final String secret;

    public MockPayChannel(@Value("${payment.secret}") String secret) {
        this.secret = secret;
    }

    @Override
    public String channel() {
        return CHANNEL_MOCK;
    }

    @Override
    public PayNotifyDTO pay(Payment payment) {
        PayNotifyDTO dto = new PayNotifyDTO();
        dto.setPaymentNo(payment.getPaymentNo());
        dto.setTradeNo(generateTradeNo());
        dto.setAmount(payment.getAmount());
        dto.setSign(sign(dto.getPaymentNo(), dto.getTradeNo(), dto.getAmount()));
        return dto;
    }

    @Override
    public boolean verifyNotify(PayNotifyDTO dto) {
        try {
            String expected = sign(dto.getPaymentNo(), dto.getTradeNo(), dto.getAmount());
            // 常量时间比较，防止时序侧信道猜测签名
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    dto.getSign().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String refund(Payment payment) {
        // 模拟渠道退款即时成功，返回退款流水号
        return "MOCKR" + System.currentTimeMillis()
                + String.valueOf(ThreadLocalRandom.current().nextInt(100, 1000));
    }

    private String sign(String paymentNo, String tradeNo, BigDecimal amount) {
        try {
            String data = "paymentNo=" + paymentNo
                    + "&tradeNo=" + tradeNo
                    + "&amount=" + amount.toPlainString();
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("支付签名生成失败", e);
        }
    }

    /**模拟第三方支付流水号*/
    private String generateTradeNo() {
        return "MOCK" + System.currentTimeMillis()
                + String.valueOf(ThreadLocalRandom.current().nextInt(100, 1000));
    }
}
