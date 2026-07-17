package backend.auth.security;

import backend.common.exception.BusinessException;
import backend.tenant.context.TenantContext;
import backend.tenant.context.TenantContextHolder;
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
            throw new BusinessException("1001", "unauthorized", HttpStatus.UNAUTHORIZED);
        }
        if (!PLATFORM_OPERATOR_TYPES.contains(context.operatorType())) {
            throw new BusinessException("1006", "platform access denied", HttpStatus.FORBIDDEN);
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
        throw new BusinessException("1009", "permission denied", HttpStatus.FORBIDDEN);
    }
}
