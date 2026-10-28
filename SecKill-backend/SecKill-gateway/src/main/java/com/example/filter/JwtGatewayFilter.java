package com.example.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    // 精确匹配：路径 + 允许的方法（登录、注册、刷新令牌、支付渠道回调）
    private static final Map<String, Set<HttpMethod>> EXACT_WHITE_LIST = Map.of(
            "/users", Set.of(HttpMethod.POST),
            "/users/login", Set.of(HttpMethod.POST),
            "/users/refresh", Set.of(HttpMethod.POST),
            "/payments/notify", Set.of(HttpMethod.POST));

    // 前缀匹配：仅 GET 放行（文档、健康检查、监控指标、商品浏览、秒杀活动浏览）
    // 注意：这里放行的请求不会注入身份头，下游 GatewaySignatureFilter 的白名单必须同步放行，否则会被 401
    private static final List<String> GET_PREFIX_WHITE_LIST = List.of(
            "/v3/api-docs", "/swagger-ui", "/actuator/health", "/actuator/prometheus",
            "/products", "/seckill-activities");

    private final PublicKey publicKey;
    private final StringRedisTemplate redisTemplate;
    private final String internalSecret;
    private static final String SIGNATURE_HEADER = "X-Gateway-Signature";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";

    public JwtGatewayFilter(@Value("${jwt.public-key}") String base64Secret,
                            @Value("${gateway.internal-secret}") String internalSecret,
                            StringRedisTemplate redisTemplate) {
        try{
            this.publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64Secret)));
        }catch (Exception e){
            throw new IllegalArgumentException("公钥解析失败，请检查jwt.public-key配置",e);
        }
        this.redisTemplate = redisTemplate;
        this.internalSecret =internalSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String timestamp = String.valueOf(System.currentTimeMillis());
        // 0. 安全前置:剥离客户端伪造的身份头,防止绕过网关注入
        ServerHttpRequest cleaned = request.mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-Username");
                    h.remove("X-Role");
                    h.remove("X-Status");
                })
                .build();
        exchange = exchange.mutate().request(cleaned).build();
        request = cleaned;

        // 1. 白名单 + 商品浏览(GET /products)直接放行
        if (isWhite(path, request.getMethod())) {
            return chain.filter(exchange);
        }

        // 2. 验签:解析 JWT
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(publicKey).build()
                    .parseSignedClaims(auth.substring(7)).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange);   // 过期/篡改/格式错误统一 401
        }

        // 3. 查黑名单:已登出的 token 直接拒
        String jti = claims.getId();
        if (jti != null && Boolean.TRUE.equals(
                redisTemplate.hasKey("token:blacklist:" + jti))) {
            return unauthorized(exchange);
        }
        String disabledAt = redisTemplate.opsForValue().get("user:blacklist:" + claims.getSubject());
        if(disabledAt != null
                && claims.getIssuedAt() != null
                && claims.getIssuedAt().getTime() <= Long.parseLong(disabledAt)){
            return unauthorized(exchange);
        }
        // 4. 登出请求:把 jti 拉黑,剩余有效期作为黑名单 TTL
        if ("/users/logout".equals(path) && jti != null) {
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue()
                        .set("token:blacklist:" + jti, "1", ttl, TimeUnit.MILLISECONDS);
            }
        }

        // 5. 身份注入请求头,转发给下游服务(对应 UserContext 读的四个头)
        ServerHttpRequest mutated = request.mutate()
                .headers(h -> {
                    h.set("X-User-Id", claims.getSubject());
                    h.set("X-Username", String.valueOf(claims.get("username")));
                    h.set("X-Role", String.valueOf(claims.get("role")));
                    h.set("X-Status", String.valueOf(claims.get("status")));
                    h.set("X-Gateway-Timestamp", timestamp);
                    h.set(SIGNATURE_HEADER, generateSignature(claims.getSubject(), String.valueOf(claims.get("role")),timestamp));
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhite(String path, HttpMethod method) {
        Set<HttpMethod> methods = EXACT_WHITE_LIST.get(path);  // 精确匹配
        if (methods != null && methods.contains(method)) {
            return true;
        }
        return HttpMethod.GET.equals(method)
                && GET_PREFIX_WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"code\":401,\"message\":\"未登录或登录已过期\"}"
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private String generateSignature(String userId, String role,String timestamp) {
        try {
            String data = "X-User-Id=" + userId + "&X-Role=" + role + "&X-Timestamp=" + timestamp;
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(new SecretKeySpec(internalSecret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}