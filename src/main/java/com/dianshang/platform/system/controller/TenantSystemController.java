package com.dianshang.platform.system.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.live.application.LiveApplicationService;
import com.dianshang.platform.notification.application.NotificationApplicationService;
import com.dianshang.platform.notification.application.NotificationGatewayProperties;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService;
import com.dianshang.platform.system.dto.HealthInfo;
import com.dianshang.platform.tenant.TenantContext;
import com.dianshang.platform.tenant.TenantContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/tenant/system")
public class TenantSystemController {

    private final AuditLogService auditLogService;
    private final NotificationApplicationService notificationApplicationService;
    private final NotificationGatewayProperties notificationGatewayProperties;
    private final OpenPlatformApplicationService openPlatformApplicationService;
    private final LiveApplicationService liveApplicationService;
    private final Environment environment;

    public TenantSystemController(AuditLogService auditLogService,
                                  NotificationApplicationService notificationApplicationService,
                                  NotificationGatewayProperties notificationGatewayProperties,
                                  OpenPlatformApplicationService openPlatformApplicationService,
                                  LiveApplicationService liveApplicationService,
                                  Environment environment) {
        this.auditLogService = auditLogService;
        this.notificationApplicationService = notificationApplicationService;
        this.notificationGatewayProperties = notificationGatewayProperties;
        this.openPlatformApplicationService = openPlatformApplicationService;
        this.liveApplicationService = liveApplicationService;
        this.environment = environment;
    }

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

    @GetMapping("/observability-overview")
    public ApiResponse<ObservabilityOverview> observabilityOverview() {
        TenantContext context = TenantContextHolder.get();
        String tenantId = context == null ? "unknown" : context.tenantId();
        List<String> latestTraceIds = auditLogService.findByTenantId(tenantId).stream()
                .map(record -> record.traceId())
                .filter(traceId -> traceId != null && !traceId.isBlank())
                .distinct()
                .limit(5)
                .toList();
        int failedNotificationCount = (int) notificationApplicationService.listNotifications(tenantId).stream()
                .filter(task -> "failed".equals(task.sendStatus()) || "dead_letter".equals(task.sendStatus()))
                .count();
        int rejectedOpenCallCount = (int) openPlatformApplicationService.listCallLogs(tenantId).stream()
                .filter(log -> log.resultStatus() != null && log.resultStatus().startsWith("rejected"))
                .count();
        int liveRiskEventCount = liveApplicationService.listLiveRiskEvents(tenantId).size();
        return ApiResponse.success(
                new ObservabilityOverview(
                        tenantId,
                        auditLogService.findByTenantId(tenantId).size(),
                        failedNotificationCount,
                        openPlatformApplicationService.listCallLogs(tenantId).size(),
                        rejectedOpenCallCount,
                        liveRiskEventCount,
                        failedNotificationCount + rejectedOpenCallCount + liveRiskEventCount,
                        latestTraceIds,
                        notificationGatewayProperties.listProviders().size(),
                        notificationGatewayProperties.enabledProviderCount(),
                        notificationGatewayProperties.mockProviderCount(),
                        isPrometheusEndpointEnabled(),
                        resolveSupportedDeploymentModes()
                ),
                TraceIdHolder.get()
        );
    }

    private boolean isPrometheusEndpointEnabled() {
        String exposure = environment.getProperty("management.endpoints.web.exposure.include", "");
        return Arrays.stream(exposure.split(","))
                .map(String::trim)
                .anyMatch("prometheus"::equals);
    }

    private List<String> resolveSupportedDeploymentModes() {
        return Arrays.stream(environment.getProperty("app.deployment.supported-modes", "standard-saas,private-deployment").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}

record ObservabilityOverview(
        String tenantId,
        int auditLogCount,
        int failedNotificationCount,
        int openPlatformCallLogCount,
        int rejectedOpenPlatformCallCount,
        int liveRiskEventCount,
        int activeAlertCount,
        List<String> latestTraceIds,
        int configuredGatewayCount,
        int enabledGatewayCount,
        int mockGatewayCount,
        boolean prometheusEndpointEnabled,
        List<String> supportedDeploymentModes
) {
}
