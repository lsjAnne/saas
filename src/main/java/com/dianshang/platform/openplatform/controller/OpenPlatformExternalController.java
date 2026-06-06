package com.dianshang.platform.openplatform.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.finance.application.FinanceService;
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

    public OpenPlatformExternalController(OpenPlatformApplicationService openPlatformApplicationService,
                                          FinanceService financeService) {
        this.openPlatformApplicationService = openPlatformApplicationService;
        this.financeService = financeService;
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
}
