package com.example.security;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisTokenBlacklist implements TokenBlacklist{
    private static final String KEY_PREFIX="seckill:token:blacklist:";
    private final StringRedisTemplate redisTemplate;

    public RedisTokenBlacklist(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklist(String jti,long ttlMillis){
        redisTemplate.opsForValue().set(KEY_PREFIX + jti,"1",ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isBlacklisted(String jti){
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX+jti));
    }
}
