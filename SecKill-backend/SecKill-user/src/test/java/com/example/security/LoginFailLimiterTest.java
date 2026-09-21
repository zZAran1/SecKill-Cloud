package com.example.security;

import com.example.exception.UserException;
import com.example.result.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginFailLimiterTest {

    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOperations;

    LoginFailLimiter limiter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        limiter = new LoginFailLimiter(redisTemplate);
    }

    @Test
    void 失败4次_不锁定() {
        when(valueOperations.get(anyString())).thenReturn("4");
        assertDoesNotThrow(() -> limiter.checkNotLocked("13800000000"));
    }

    @Test
    void 失败5次_抛锁定异常() {
        when(valueOperations.get(anyString())).thenReturn("5");
        UserException e = assertThrows(UserException.class,
                () -> limiter.checkNotLocked("13800000000"));
        assertEquals(ErrorCode.LOGIN_LOCKED, e.getErrorCode());
    }

    @Test
    void 首次失败_开启10分钟窗口() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        limiter.onFail("13800000000");
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void 非首次失败_不刷新窗口() {
        when(valueOperations.increment(anyString())).thenReturn(3L);
        limiter.onFail("13800000000");
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void 登录成功_清零计数() {
        limiter.onSuccess("13800000000");
        verify(redisTemplate).delete(contains("13800000000"));
    }
}
