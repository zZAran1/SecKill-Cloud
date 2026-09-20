package com.example.security;

import com.example.exception.UserException;
import com.example.result.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;



@Component
public class LoginFailLimiter {
    private static final String KEY_PREFIX="seckill:login:fail:";
    private static final int MAX_FAILS=5;
    private static final Duration WINDOW =Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public LoginFailLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkNotLocked(String phone){
        String fails=redisTemplate.opsForValue().get(KEY_PREFIX+phone);
        if(fails!=null && Integer.parseInt(fails)>=MAX_FAILS){
            throw new UserException(ErrorCode.LOGIN_LOCKED);
        }
    }

    public void onFail(String phone){
        String key= KEY_PREFIX + phone;
        Long count =redisTemplate.opsForValue().increment(key);
        if(count!=null && count ==1L){
            redisTemplate.expire(key,WINDOW);
        }
    }

    public void onSuccess(String phone){
        redisTemplate.delete(KEY_PREFIX+phone);
    }
}
