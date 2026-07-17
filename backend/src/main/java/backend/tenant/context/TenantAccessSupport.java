package backend.tenant.context;

import backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class TenantAccessSupport {

    private TenantAccessSupport() {
    }

    public static TenantContext requiredContext() {
        TenantContext context = TenantContextHolder.get();
        if (context == null) {
            throw new BusinessException("1001", "unauthorized", HttpStatus.UNAUTHORIZED);
        }
        return context;
    }

    public static String requiredTenantId() {
        String tenantId = requiredContext().tenantId();
        if (tenantId == null || tenantId.isBlank() || "platform".equals(tenantId)) {
            throw new BusinessException("1004", "tenant context missing", HttpStatus.BAD_REQUEST);
        }
        return tenantId;
    }

    public static void assertSameTenant(String tenantId) {
        if (!requiredTenantId().equals(tenantId)) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
    }
}
