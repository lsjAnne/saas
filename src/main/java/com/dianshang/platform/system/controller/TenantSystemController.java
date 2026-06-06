package com.dianshang.platform.system.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.system.dto.HealthInfo;
import com.dianshang.platform.tenant.TenantContext;
import com.dianshang.platform.tenant.TenantContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/system")
public class TenantSystemController {

    @GetMapping("/health")
    public ApiResponse<HealthInfo> health() {
        TenantContext context = TenantContextHolder.get();
        HealthInfo healthInfo = new HealthInfo(
                "dianShangPingTai",
                "UP",
                context == null ? "unknown" : context.tenantId()
        );
        return ApiResponse.success(healthInfo, TraceIdHolder.get());
    }
}
