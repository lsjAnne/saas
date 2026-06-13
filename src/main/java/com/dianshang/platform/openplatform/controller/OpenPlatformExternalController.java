package com.dianshang.platform.openplatform.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.dashboard.application.DashboardService;
import com.dianshang.platform.dashboard.application.DashboardService.BiExternalPlatformOverviewView;
import com.dianshang.platform.finance.application.FinanceService;
import com.dianshang.platform.fulfillment.application.FulfillmentService;
import com.dianshang.platform.inventory.application.InventoryService;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService.ExternalAppProfile;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService.ExternalDeliveryReadinessOverviewView;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService.ExternalMessagingCallbackBridgeOverviewView;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService.ExternalObservabilityReadinessOverviewView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OpenPlatformExternalController {

    private final OpenPlatformApplicationService openPlatformApplicationService;
    private final DashboardService dashboardService;
    private final FinanceService financeService;
    private final InventoryService inventoryService;
    private final FulfillmentService fulfillmentService;

    public OpenPlatformExternalController(OpenPlatformApplicationService openPlatformApplicationService,
                                          DashboardService dashboardService,
                                          FinanceService financeService,
                                          InventoryService inventoryService,
                                          FulfillmentService fulfillmentService) {
        this.openPlatformApplicationService = openPlatformApplicationService;
        this.dashboardService = dashboardService;
        this.financeService = financeService;
        this.inventoryService = inventoryService;
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping("/api/open/external/profile")
    public ApiResponse<ExternalAppProfile> profile(@RequestHeader("X-Open-App-Key") String accessKey,
                                                   @RequestHeader("X-Open-App-Secret") String secret) {
        return ApiResponse.success(
                openPlatformApplicationService.authenticateExternalProfile(
                        accessKey,
                        secret,
                        "/api/open/external/profile"
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/erp/master-data-dictionaries")
    public ApiResponse<List<FinanceService.ErpMasterDataDictionaryEntry>> listErpMasterDataDictionaries(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                                                          @RequestHeader("X-Open-App-Secret") String secret,
                                                                                                          @RequestParam(required = false) String dictionaryType) {
        ExternalAppProfile profile = openPlatformApplicationService.authorizeExternalErpMasterDataRead(
                accessKey,
                secret,
                "/api/open/external/erp/master-data-dictionaries"
        );
        return ApiResponse.success(
                financeService.listErpMasterDataDictionaries(profile.tenantId(), dictionaryType),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/erp/account-mappings")
    public ApiResponse<List<FinanceService.ErpAccountMappingEntry>> listErpAccountMappings(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                                             @RequestHeader("X-Open-App-Secret") String secret,
                                                                                             @RequestParam(required = false) String mappingCategory,
                                                                                             @RequestParam(required = false) String storeId) {
        ExternalAppProfile profile = openPlatformApplicationService.authorizeExternalErpAccountMappingRead(
                accessKey,
                secret,
                "/api/open/external/erp/account-mappings"
        );
        return ApiResponse.success(
                financeService.listErpAccountMappings(profile.tenantId(), mappingCategory, storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/erp/integration-baseline")
    public ApiResponse<FinanceService.ErpIntegrationBaselineView> getErpIntegrationBaseline(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                                              @RequestHeader("X-Open-App-Secret") String secret) {
        ExternalAppProfile profile = openPlatformApplicationService.authorizeExternalErpIntegrationBaselineRead(
                accessKey,
                secret,
                "/api/open/external/erp/integration-baseline"
        );
        return ApiResponse.success(
                financeService.getErpIntegrationBaseline(profile.tenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/wms/linkage")
    public ApiResponse<InventoryService.WmsLinkageView> getWmsLinkage(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                      @RequestHeader("X-Open-App-Secret") String secret,
                                                                      @RequestParam String storeId) {
        ExternalAppProfile profile = openPlatformApplicationService.authorizeExternalWmsLinkageRead(
                accessKey,
                secret,
                "/api/open/external/wms/linkage"
        );
        return ApiResponse.success(
                inventoryService.getWmsLinkage(profile.tenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/tms/control-tower")
    public ApiResponse<FulfillmentService.ControlTowerView> getTmsControlTower(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                               @RequestHeader("X-Open-App-Secret") String secret) {
        ExternalAppProfile profile = openPlatformApplicationService.authorizeExternalTmsControlTowerRead(
                accessKey,
                secret,
                "/api/open/external/tms/control-tower"
        );
        return ApiResponse.success(
                fulfillmentService.getControlTower(profile.tenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/erp/ofbiz-baseline")
    public ApiResponse<OpenPlatformApplicationService.ExternalErpBaselineView> getExternalErpOfbizBaseline(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                                                            @RequestHeader("X-Open-App-Secret") String secret) {
        openPlatformApplicationService.authorizeExternalErpOfbizBaselineRead(
                accessKey,
                secret,
                "/api/open/external/erp/ofbiz-baseline"
        );
        return ApiResponse.success(
                openPlatformApplicationService.getExternalErpOfbizBaseline(),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/wms/openboxes-baseline")
    public ApiResponse<OpenPlatformApplicationService.ExternalWmsBaselineView> getExternalWmsOpenboxesBaseline(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                                                                @RequestHeader("X-Open-App-Secret") String secret) {
        openPlatformApplicationService.authorizeExternalWmsOpenboxesBaselineRead(
                accessKey,
                secret,
                "/api/open/external/wms/openboxes-baseline"
        );
        return ApiResponse.success(
                openPlatformApplicationService.getExternalWmsOpenboxesBaseline(),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/messaging/rabbitmq-baseline")
    public ApiResponse<OpenPlatformApplicationService.ExternalMessagingBaselineView> getExternalMessagingRabbitMqBaseline(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                                                                          @RequestHeader("X-Open-App-Secret") String secret) {
        openPlatformApplicationService.authorizeExternalMessagingRabbitMqBaselineRead(
                accessKey,
                secret,
                "/api/open/external/messaging/rabbitmq-baseline"
        );
        return ApiResponse.success(
                openPlatformApplicationService.getExternalMessagingRabbitMqBaseline(),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/bi/superset-overview")
    public ApiResponse<BiExternalPlatformOverviewView> getExternalBiSupersetOverview(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                                     @RequestHeader("X-Open-App-Secret") String secret) {
        ExternalAppProfile profile = openPlatformApplicationService.authorizeExternalBiSupersetOverviewRead(
                accessKey,
                secret,
                "/api/open/external/bi/superset-overview"
        );
        return ApiResponse.success(
                dashboardService.getExternalBiPlatformOverview(profile.tenantId(), null),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/messaging/callback-bridge")
    public ApiResponse<ExternalMessagingCallbackBridgeOverviewView> getExternalMessagingCallbackBridge(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                                                        @RequestHeader("X-Open-App-Secret") String secret) {
        ExternalAppProfile profile = openPlatformApplicationService.authorizeExternalMessagingCallbackBridgeRead(
                accessKey,
                secret,
                "/api/open/external/messaging/callback-bridge"
        );
        return ApiResponse.success(
                openPlatformApplicationService.getExternalMessagingCallbackBridgeOverview(profile.tenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/system/observability-readiness")
    public ApiResponse<ExternalObservabilityReadinessOverviewView> getExternalObservabilityReadiness(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                                                     @RequestHeader("X-Open-App-Secret") String secret) {
        ExternalAppProfile profile = openPlatformApplicationService.authorizeExternalObservabilityReadinessRead(
                accessKey,
                secret,
                "/api/open/external/system/observability-readiness"
        );
        return ApiResponse.success(
                openPlatformApplicationService.getExternalObservabilityReadiness(profile.tenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/external/delivery/readiness")
    public ApiResponse<ExternalDeliveryReadinessOverviewView> getExternalDeliveryReadiness(@RequestHeader("X-Open-App-Key") String accessKey,
                                                                                           @RequestHeader("X-Open-App-Secret") String secret) {
        ExternalAppProfile profile = openPlatformApplicationService.authorizeExternalDeliveryReadinessRead(
                accessKey,
                secret,
                "/api/open/external/delivery/readiness"
        );
        return ApiResponse.success(
                openPlatformApplicationService.getExternalDeliveryReadiness(profile.tenantId()),
                TraceIdHolder.get()
        );
    }
}
