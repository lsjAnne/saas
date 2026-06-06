package com.dianshang.platform.system.dto;

public record HealthInfo(
        String application,
        String status,
        String tenantId
) {
}
