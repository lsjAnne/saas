package com.dianshang.platform.infra.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyBuilder {

    private final String tenantKeyPrefix;

    public RedisKeyBuilder(@Value("${app.redis.tenant-key-prefix:dsp}") String tenantKeyPrefix) {
        this.tenantKeyPrefix = tenantKeyPrefix;
    }

    public String buildTenantKey(String tenantId, String module, String key) {
        return tenantKeyPrefix + ":" + tenantId + ":" + module + ":" + key;
    }

    public String buildIdempotencyKey(String tenantId, String action, String idempotencyKey) {
        return buildTenantKey(tenantId, "idempotent", action + ":" + idempotencyKey);
    }
}
