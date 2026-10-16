package com.example.security;

import com.example.exception.BaseBusinessException;
import com.example.result.ErrorCode;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewaySignatureFilter implements Filter {

    private static final String SIGNATURE_HEADER = "X-Gateway-Signature";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private final String internalSecret;

    public GatewaySignatureFilter(@Value("${gateway.internal-secret}") String internalSecret) {
        this.internalSecret = internalSecret;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();

        // 内部服务间接口：校验内部签名（与用户身份签名分离）
        if (path.startsWith("/internal/")) {
            String sig = req.getHeader("X-Internal-Signature");
            if (sig == null || !verifyInternalSignature(sig)) {
                writeUnauthorized((HttpServletResponse) response);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 白名单放行（健康检查、Swagger、注册登录）
        if (isWhite(path, req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String expected = req.getHeader(SIGNATURE_HEADER);
        if (expected == null || expected.isBlank()) {
            writeUnauthorized((HttpServletResponse) response);
            return;
        }

        String userId = req.getHeader("X-User-Id");
        String role = req.getHeader("X-Role");
        if (!verifySignature(userId, role, expected)) {
            writeUnauthorized((HttpServletResponse) response);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean verifyInternalSignature(String expected) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(new SecretKeySpec(internalSecret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM));
            byte[] bytes = mac.doFinal("INTERNAL-CALL".getBytes(StandardCharsets.UTF_8));
            String actual = Base64.getEncoder().encodeToString(bytes);
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
    private boolean isWhite(String path, String method) {
        return path.startsWith("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || (path.equals("/users") && "POST".equalsIgnoreCase(method));
    }

    private boolean verifySignature(String userId, String role, String expected) {
        try {
            String data = "X-User-Id=" + userId + "&X-Role=" + role;
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(new SecretKeySpec(internalSecret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String actual = Base64.getEncoder().encodeToString(bytes);
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
    }
}