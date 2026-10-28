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
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewaySignatureFilter implements Filter {

    private static final String SIGNATURE_HEADER = "X-Gateway-Signature";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";

    /**公开浏览路径：仅 GET 放行，必须与网关 JwtGatewayFilter.GET_PREFIX_WHITE_LIST 保持一致*/
    private static final List<String> GET_PREFIX_WHITE_LIST = List.of(
            "/products", "/seckill-activities");

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
        String timestamp = req.getHeader("X-Gateway-Timestamp");
        if(timestamp == null || !isTimestampValid(timestamp) || !verifySignature(userId,role,expected,timestamp)) {
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
        // Prometheus 抓指标走容器网络直连服务端口，无网关签名，需放行（指标不含业务敏感数据）
        if (path.startsWith("/actuator/health")
                || path.startsWith("/actuator/prometheus")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")) {
            return true;
        }
        if ((path.equals("/users") || path.equals("/users/login") || path.equals("/users/refresh"))
                && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        // 支付渠道回调：无登录态的公开接口，安全靠回调报文签名（payment.secret）保证，
        // 与网关 JwtGatewayFilter.EXACT_WHITE_LIST 保持一致
        if (path.equals("/payments/notify") && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        // 公开浏览接口：网关放行后不注入身份头，这里必须同步放行，否则匿名浏览会被 401
        // 与网关 JwtGatewayFilter.GET_PREFIX_WHITE_LIST 保持一致
        return "GET".equalsIgnoreCase(method)
                && GET_PREFIX_WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private boolean verifySignature(String userId, String role, String expected ,String timestamp) {
        try {
            String data = "X-User-Id=" + userId + "&X-Role=" + role + "&X-Timestamp=" + timestamp;
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

    private boolean isTimestampValid(String timestamp) {
        try{
            long ts = Long.parseLong(timestamp);
            return Math.abs(System.currentTimeMillis()-ts)<= 5*60*1000L;
        }catch (NumberFormatException e){
            return false;
        }
    }
}