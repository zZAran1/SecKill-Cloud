package com.example.security;

import com.example.constant.UserConstants;
import com.example.exception.UserException;
import com.example.mapper.UserMapper;
import com.example.model.entity.Users;
import com.example.model.vo.TokenVO;
import com.example.result.ErrorCode;
import com.example.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * 刷新令牌管理：双 token 机制（短时效 access token + 长时效 refresh token）。
 *
 * refresh token 是随机串（非 JWT），只存 Redis：
 * - 可随时吊销（登出/封禁/改密码即删）；
 * - 刷新时一次性使用（GETDEL 原子取删），旧 token 重放直接拒绝，防止泄露后被反复续期。
 *
 * Redis 双向索引：
 * - refresh:token:{token} -> userId（刷新时按 token 找人）
 * - refresh:user:{userId} -> token（登出时按人吊销）
 */
@Component
public class RefreshTokenManager {

    private static final String REFRESH_TOKEN_KEY = "refresh:token:";
    private static final String REFRESH_USER_KEY = "refresh:user:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    /**refresh token 有效期（毫秒）*/
    private final long refreshTtlMillis;
    /**access token 有效期（毫秒），仅回填给前端做续期参考*/
    private final long accessTtlMillis;

    public RefreshTokenManager(StringRedisTemplate redisTemplate,
                               JwtUtil jwtUtil,
                               UserMapper userMapper,
                               @Value("${jwt.refresh-expiration}") long refreshTtlMillis,
                               @Value("${jwt.expiration}") long accessTtlMillis) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.refreshTtlMillis = refreshTtlMillis;
        this.accessTtlMillis = accessTtlMillis;
    }

    /**登录/刷新时签发 token 对；同一用户只保留一份有效 refresh token（旧的自动吊销）*/
    public TokenVO issue(Users user) {
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername(),
                String.valueOf(user.getRole()), String.valueOf(user.getStatus()));

        // 轮转：先吊销旧 refresh，避免多份并存
        revoke(user.getId());
        String refreshToken = generateRefreshToken();
        Duration ttl = Duration.ofMillis(refreshTtlMillis);
        redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY + refreshToken, String.valueOf(user.getId()), ttl);
        redisTemplate.opsForValue().set(REFRESH_USER_KEY + user.getId(), refreshToken, ttl);

        TokenVO vo = new TokenVO();
        vo.setToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(accessTtlMillis / 1000);
        return vo;
    }

    /**
     * 用 refresh token 换新 token 对（轮转）。
     * 旧 token 一次性使用：并发/重放的第二次调用拿不到记录，直接 401。
     */
    public TokenVO refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new UserException(ErrorCode.UNAUTHORIZED);
        }
        String userIdStr = redisTemplate.opsForValue().getAndDelete(REFRESH_TOKEN_KEY + refreshToken);
        if (userIdStr == null) {
            // 无效或已使用（重放），统一按登录态失效处理
            throw new UserException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(userIdStr);
        redisTemplate.delete(REFRESH_USER_KEY + userId);

        Users user = userMapper.selectById(userId);
        // 每次刷新都重查用户：角色/封禁状态变化立即反映到新 access token
        if (user == null || user.getStatus() != UserConstants.STATUS_ENABLED) {
            throw new UserException(ErrorCode.ACCOUNT_DISABLED);
        }
        return issue(user);
    }

    /**登出/需强制下线时吊销该用户的 refresh token*/
    public void revoke(Long userId) {
        String token = redisTemplate.opsForValue().getAndDelete(REFRESH_USER_KEY + userId);
        if (token != null) {
            redisTemplate.delete(REFRESH_TOKEN_KEY + token);
        }
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
