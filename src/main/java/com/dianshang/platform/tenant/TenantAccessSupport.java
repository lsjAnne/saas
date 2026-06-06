package com.dianshang.platform.tenant;

import com.dianshang.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class TenantAccessSupport {

    private TenantAccessSupport() {
    }

    public static TenantContext requiredContext() {
        TenantContext context = TenantContextHolder.get();
        if (context == null) {
            throw new BusinessException("1001", "未登录", HttpStatus.UNAUTHORIZED);
        }
        return context;
    }

    public static String requiredTenantId() {
        String tenantId = requiredContext().tenantId();
        if (tenantId == null || tenantId.isBlank() || "platform".equals(tenantId)) {
            throw new BusinessException("1004", "租户上下文缺失", HttpStatus.BAD_REQUEST);
        }
        return tenantId;
    }

    public static void assertSameTenant(String tenantId) {
        if (!requiredTenantId().equals(tenantId)) {
            throw new BusinessException("1005", "租户上下文非法切换", HttpStatus.FORBIDDEN);
        }
    }
}
