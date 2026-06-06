package com.dianshang.platform.tenant;

import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Set;

public final class PlatformAccessSupport {

    private static final Set<String> PLATFORM_OPERATOR_TYPES = Set.of(
            "platform-admin",
            "platform-ops",
            "platform-support",
            "platform-system"
    );

    private PlatformAccessSupport() {
    }

    public static TenantContext requiredPlatformContext() {
        TenantContext context = TenantContextHolder.get();
        if (context == null) {
            throw new BusinessException("1001", "未登录", HttpStatus.UNAUTHORIZED);
        }
        if (!PLATFORM_OPERATOR_TYPES.contains(context.operatorType())) {
            throw new BusinessException("1006", "无权访问平台级数据", HttpStatus.FORBIDDEN);
        }
        return context;
    }

    public static void requireAny(String... operatorTypes) {
        TenantContext context = requiredPlatformContext();
        if (context.hasPermission(AuthPermissionCodes.PLATFORM_TENANT_MANAGE)
                || context.hasPermission(AuthPermissionCodes.PLATFORM_SUPPORT_MANAGE)) {
            for (String operatorType : operatorTypes) {
                if (operatorType.equals(context.operatorType())) {
                    return;
                }
            }
        }
        for (String operatorType : operatorTypes) {
            if (operatorType.equals(context.operatorType())) {
                return;
            }
        }
        throw new BusinessException("1009", "权限不足", HttpStatus.FORBIDDEN);
    }
}
