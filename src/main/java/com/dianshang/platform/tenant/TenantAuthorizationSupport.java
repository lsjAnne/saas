package com.dianshang.platform.tenant;

import com.dianshang.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class TenantAuthorizationSupport {

    private TenantAuthorizationSupport() {
    }

    public static TenantContext requirePermission(String permissionCode) {
        TenantContext context = TenantAccessSupport.requiredContext();
        if ("platform".equals(context.tenantId())) {
            throw new BusinessException("1004", "租户上下文缺失", HttpStatus.BAD_REQUEST);
        }
        if (!context.hasPermission(permissionCode)) {
            throw new BusinessException("1009", "权限不足", HttpStatus.FORBIDDEN);
        }
        return context;
    }
}
