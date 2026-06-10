package com.dianshang.platform.openplatform.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.finance.application.FinanceService;
import com.dianshang.platform.fulfillment.application.FulfillmentService;
import com.dianshang.platform.inventory.application.InventoryService;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService.ExternalAppProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OpenPlatformExternalController {

    private final OpenPlatformApplicationService openPlatformApplicationService;
    private final FinanceService financeService;
    private final InventoryService inventoryService;
    private final FulfillmentService fulfillmentService;

    public OpenPlatformExternalController(OpenPlatformApplicationService openPlatformApplicationService,
                                          FinanceService financeService,
                                          InventoryService inventoryService,
                                          FulfillmentService fulfillmentService) {
        this.openPlatformApplicationService = openPlatformApplicationService;
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
}
