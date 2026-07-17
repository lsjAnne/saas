package backend.auth.security;

import backend.common.exception.BusinessException;
import backend.tenant.context.TenantContext;
import backend.tenant.context.TenantAccessSupport;
import org.springframework.http.HttpStatus;

public final class TenantAuthorizationSupport {

    private TenantAuthorizationSupport() {
    }

    public static TenantContext requirePermission(String permissionCode) {
        TenantContext context = TenantAccessSupport.requiredContext();
        if ("platform".equals(context.tenantId())) {
            throw new BusinessException("1004", "tenant context missing", HttpStatus.BAD_REQUEST);
        }
        if (!context.hasPermission(permissionCode)) {
            throw new BusinessException("1009", "permission denied", HttpStatus.FORBIDDEN);
        }
        return context;
    }
}
