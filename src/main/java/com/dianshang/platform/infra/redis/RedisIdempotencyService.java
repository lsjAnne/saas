package com.dianshang.platform.infra.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisIdempotencyService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisIdempotencyService(StringRedisTemplate stringRedisTemplate, RedisKeyBuilder redisKeyBuilder) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public boolean markIfAbsent(String tenantId, String action, String idempotencyKey, Duration ttl) {
        String redisKey = redisKeyBuilder.buildIdempotencyKey(tenantId, action, idempotencyKey);
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", ttl);
        return Boolean.TRUE.equals(success);
    }

    public boolean exists(String tenantId, String action, String idempotencyKey) {
        String redisKey = redisKeyBuilder.buildIdempotencyKey(tenantId, action, idempotencyKey);
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        return Boolean.TRUE.equals(exists);
    }
}
