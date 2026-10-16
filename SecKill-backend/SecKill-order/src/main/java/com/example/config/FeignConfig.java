package com.example.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class FeignConfig {

    @Value("${gateway.internal-secret}")
    private String internalSecret;

    @Bean
    public RequestInterceptor internalSignatureInterceptor() {
        return template -> {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(internalSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] bytes = mac.doFinal("INTERNAL-CALL".getBytes(StandardCharsets.UTF_8));
                template.header("X-Internal-Signature", Base64.getEncoder().encodeToString(bytes));
            } catch (Exception e) {
                throw new IllegalStateException("生成内部签名失败", e);
            }
        };
    }
}