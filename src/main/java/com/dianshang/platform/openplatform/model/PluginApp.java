package com.dianshang.platform.openplatform.model;

import java.time.OffsetDateTime;
import java.util.List;

public record PluginApp(
        String appId,
        String organizationId,
        String appName,
        String appType,
        List<String> permissionScope,
        String accessKey,
        String secretMasked,
        String status,
        OffsetDateTime createdAt
) {
}
