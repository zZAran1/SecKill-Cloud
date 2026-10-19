package com.example.constant;

import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 秒杀相关的 Redis key 与 Lua 脚本，统一维护避免各处硬编码导致 key 漂移
 */
public final class SeckillRedisKeys {

    /**活动信息 JSON*/
    public static final String ACTIVITY_KEY = "seckill:activity:";
    /**活动剩余库存*/
    public static final String STOCK_KEY = "seckill:stock:";
    /**已参与用户集合（每人限购一次）*/
    public static final String USERS_KEY = "seckill:users:";

    /**
     * 回补库存：库存 +1 并解除该用户的限购标记。
     * 订单取消/超时关闭/建单失败走死信时都要调用，必须原子执行。
     */
    public static final DefaultRedisScript<Long> RESTORE_SCRIPT = new DefaultRedisScript<>();

    static {
        RESTORE_SCRIPT.setScriptText("""
                redis.call('INCR',KEYS[1])
                redis.call('SREM',KEYS[2],ARGV[1])
                return 1
                """);
        RESTORE_SCRIPT.setResultType(Long.class);
    }

    private SeckillRedisKeys() {
    }
}
