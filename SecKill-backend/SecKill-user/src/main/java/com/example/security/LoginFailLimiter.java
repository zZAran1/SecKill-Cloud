package com.example.security;

import com.example.exception.UserException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;



@Component
public class LoginFailLimiter {
    private static final String KET_PREFIX="seckill:login:fail:";
    private static final int MAX_FAILS=5;
    private static final Duration WINDOW =Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public LoginFailLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkNotLocked(String phone){
        String fails=redisTemplate.opsForValue().get(KET_PREFIX+phone);
        if(fails!=null && Integer.parseInt(fails)>=MAX_FAILS){
            throw new UserException("失败次数过多，账号已锁定，请10分钟后再试");
        }
    }

    public void onFail(String phone){
        String key= KET_PREFIX + phone;
        Long count =redisTemplate.opsForValue().increment(key);
        if(count!=null && count ==1L){
            redisTemplate.expire(key,WINDOW);
        }
    }

    public void onSuccess(String phone){
        redisTemplate.delete(KET_PREFIX+phone);
    }
}
