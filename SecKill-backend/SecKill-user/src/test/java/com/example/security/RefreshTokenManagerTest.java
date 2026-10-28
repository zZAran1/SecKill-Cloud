package com.example.security;

import com.example.constant.UserConstants;
import com.example.exception.UserException;
import com.example.mapper.UserMapper;
import com.example.model.entity.Users;
import com.example.model.vo.TokenVO;
import com.example.result.ErrorCode;
import com.example.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 双 token 机制测试：签发、一次性轮转（重放拒绝）、封禁拦截、登出吊销。
 */
class RefreshTokenManagerTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private JwtUtil jwtUtil;
    private UserMapper userMapper;
    private RefreshTokenManager manager;

    private Users buildUser() {
        Users user = new Users();
        user.setId(1L);
        user.setUsername("tester");
        user.setRole(UserConstants.ROLE_USER);
        user.setStatus(UserConstants.STATUS_ENABLED);
        return user;
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        jwtUtil = mock(JwtUtil.class);
        userMapper = mock(UserMapper.class);
        manager = new RefreshTokenManager(redisTemplate, jwtUtil, userMapper, 604800000L, 7200000L);

        when(jwtUtil.generateToken(eq(1L), anyString(), anyString(), anyString())).thenReturn("ACCESS_TOKEN");
    }

    @Test
    void 签发_返回双token并写入Redis索引() {
        TokenVO vo = manager.issue(buildUser());

        assertEquals("ACCESS_TOKEN", vo.getToken());
        assertNotNull(vo.getRefreshToken());
        assertEquals(7200L, vo.getExpiresIn());
        // 双向索引都要写入
        verify(valueOperations).set(eq("refresh:token:" + vo.getRefreshToken()), eq("1"), any());
        verify(valueOperations).set(eq("refresh:user:1"), eq(vo.getRefreshToken()), any());
    }

    @Test
    void 刷新_旧token一次性使用_成功换新() {
        when(valueOperations.getAndDelete("refresh:token:OLD")).thenReturn("1");
        when(userMapper.selectById(1L)).thenReturn(buildUser());
        when(valueOperations.getAndDelete("refresh:user:1")).thenReturn("OLD");

        TokenVO vo = manager.refresh("OLD");

        assertEquals("ACCESS_TOKEN", vo.getToken());
        assertNotNull(vo.getRefreshToken());
    }

    @Test
    void 刷新_重放旧token_拒绝() {
        // getAndDelete 第二次拿不到：已消费过
        when(valueOperations.getAndDelete("refresh:token:OLD")).thenReturn(null);

        UserException e = assertThrows(UserException.class, () -> manager.refresh("OLD"));
        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
    }

    @Test
    void 刷新_空token_拒绝() {
        UserException e = assertThrows(UserException.class, () -> manager.refresh(" "));
        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
    }

    @Test
    void 刷新_账号已被封禁_拒绝() {
        Users disabled = buildUser();
        disabled.setStatus(UserConstants.STATUS_DISABLED);
        when(valueOperations.getAndDelete("refresh:token:OLD")).thenReturn("1");
        when(valueOperations.getAndDelete("refresh:user:1")).thenReturn("OLD");
        when(userMapper.selectById(1L)).thenReturn(disabled);

        UserException e = assertThrows(UserException.class, () -> manager.refresh("OLD"));
        assertEquals(ErrorCode.ACCOUNT_DISABLED, e.getErrorCode());
    }

    @Test
    void 登出_吊销refreshToken() {
        when(valueOperations.getAndDelete("refresh:user:1")).thenReturn("OLD");

        manager.revoke(1L);

        verify(redisTemplate).delete("refresh:token:OLD");
    }
}
