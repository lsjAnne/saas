package com.dianshang.platform.dashboard.controller;

import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.dashboard.application.DashboardService;
import com.dianshang.platform.dashboard.application.DashboardService.BiCockpitView;
import com.dianshang.platform.dashboard.application.DashboardService.BiDataQualityCheckView;
import com.dianshang.platform.dashboard.application.DashboardService.BiDeliveryChecklistView;
import com.dianshang.platform.dashboard.application.DashboardService.BiExportTaskView;
import com.dianshang.platform.dashboard.application.DashboardService.BiLayerDefinitionView;
import com.dianshang.platform.dashboard.application.DashboardService.BiMarketingAnalysisView;
import com.dianshang.platform.dashboard.application.DashboardService.BiMetricDefinitionView;
import com.dianshang.platform.dashboard.application.DashboardService.BiOperationalAnalysisView;
import com.dianshang.platform.dashboard.application.DashboardService.BiOverviewView;
import com.dianshang.platform.dashboard.application.DashboardService.BiRepairTaskView;
import com.dianshang.platform.dashboard.application.DashboardService.BiSubscriptionView;
import com.dianshang.platform.dashboard.application.DashboardService.BiThemeDomainView;
import com.dianshang.platform.dashboard.application.DashboardService.DashboardCampaignAnalysisView;
import com.dianshang.platform.dashboard.application.DashboardService.DashboardMemberAnalysisView;
import com.dianshang.platform.dashboard.application.DashboardService.DashboardRiskView;
import com.dianshang.platform.dashboard.application.DashboardService.DashboardSummaryView;
import com.dianshang.platform.dashboard.application.DashboardService.DashboardTrendView;
import com.dianshang.platform.dashboard.application.DashboardService.RepairBiDataRequest;
import com.dianshang.platform.dashboard.application.DashboardService.SaveBiExportTaskRequest;
import com.dianshang.platform.dashboard.application.DashboardService.SaveBiMetricDefinitionRequest;
import com.dianshang.platform.dashboard.application.DashboardService.SaveBiSubscriptionRequest;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.DASHBOARD_READ)
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard/summary")
    public ApiResponse<DashboardSummaryView> getSummary(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.getSummary(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/dashboard/trends")
    public ApiResponse<List<DashboardTrendView>> getTrends(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.getTrends(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/dashboard/risks")
    public ApiResponse<List<DashboardRiskView>> getRisks(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.getRisks(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/dashboard/campaign-analysis")
    public ApiResponse<DashboardCampaignAnalysisView> getCampaignAnalysis(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.getCampaignAnalysis(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/dashboard/member-analysis")
    public ApiResponse<DashboardMemberAnalysisView> getMemberAnalysis(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.getMemberAnalysis(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/theme-domains")
    public ApiResponse<List<BiThemeDomainView>> listBiThemeDomains(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.listBiThemeDomains(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/metric-dictionaries")
    public ApiResponse<List<BiMetricDefinitionView>> listBiMetricDefinitions() {
        return ApiResponse.success(
                dashboardService.listBiMetricDefinitions(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/bi/metric-dictionaries")
    public ApiResponse<BiMetricDefinitionView> saveBiMetricDefinition(@Valid @RequestBody SaveBiMetricDefinitionRequest request) {
        return ApiResponse.success(
                dashboardService.saveBiMetricDefinition(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/layers")
    public ApiResponse<List<BiLayerDefinitionView>> listBiLayers() {
        return ApiResponse.success(
                dashboardService.listBiLayers(),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/overview")
    public ApiResponse<BiOverviewView> getBiOverview(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.getBiOverview(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/operations-analysis")
    public ApiResponse<BiOperationalAnalysisView> getBiOperationalAnalysis(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.getBiOperationalAnalysis(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/marketing-analysis")
    public ApiResponse<BiMarketingAnalysisView> getBiMarketingAnalysis(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.getBiMarketingAnalysis(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/bi/export-tasks")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE)
    public ApiResponse<BiExportTaskView> saveBiExportTask(@Valid @RequestBody SaveBiExportTaskRequest request) {
        return ApiResponse.success(
                dashboardService.saveBiExportTask(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/export-tasks")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE)
    public ApiResponse<List<BiExportTaskView>> listBiExportTasks() {
        return ApiResponse.success(
                dashboardService.listBiExportTasks(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/cockpit")
    public ApiResponse<BiCockpitView> getBiCockpit(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.getBiCockpit(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/bi/subscriptions")
    public ApiResponse<BiSubscriptionView> saveBiSubscription(@Valid @RequestBody SaveBiSubscriptionRequest request) {
        return ApiResponse.success(
                dashboardService.saveBiSubscription(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/subscriptions")
    public ApiResponse<List<BiSubscriptionView>> listBiSubscriptions() {
        return ApiResponse.success(
                dashboardService.listBiSubscriptions(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/data-quality-checks")
    public ApiResponse<List<BiDataQualityCheckView>> listBiDataQualityChecks(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.listBiDataQualityChecks(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/bi/data-repair-tasks")
    public ApiResponse<BiRepairTaskView> repairBiData(@Valid @RequestBody RepairBiDataRequest request) {
        return ApiResponse.success(
                dashboardService.repairBiData(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/bi/delivery-checklist")
    public ApiResponse<BiDeliveryChecklistView> getBiDeliveryChecklist(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                dashboardService.getBiDeliveryChecklist(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }
}
