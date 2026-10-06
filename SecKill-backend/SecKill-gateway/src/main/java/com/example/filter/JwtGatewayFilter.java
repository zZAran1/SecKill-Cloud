package com.example.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    // 无需登录的白名单路径
    private static final List<String> WHITE_LIST = List.of(
            "/users/login", "/users/register",
            "/v3/api-docs", "/swagger-ui", "/actuator/health");

    private final SecretKey secretKey;
    private final StringRedisTemplate redisTemplate;

    public JwtGatewayFilter(@Value("${jwt.secret}") String base64Secret,
                            StringRedisTemplate redisTemplate) {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

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
            claims = Jwts.parser().verifyWith(secretKey).build()
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
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhite(String path, HttpMethod method) {
        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            return true;
        }
        return HttpMethod.GET.equals(method) && path.startsWith("/products");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"code\":401,\"message\":\"未登录或登录已过期\"}"
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}