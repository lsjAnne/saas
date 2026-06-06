package com.dianshang.platform.servicecase.controller;

import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.servicecase.application.ServiceCaseService;
import com.dianshang.platform.servicecase.application.ServiceCaseService.SaveTicketSatisfactionRequest;
import com.dianshang.platform.servicecase.application.ServiceCaseService.TicketSatisfactionView;
import com.dianshang.platform.servicecase.application.ServiceCaseService.TicketSlaOverviewView;
import com.dianshang.platform.servicecase.dto.CreateAfterSaleRequest;
import com.dianshang.platform.servicecase.model.AfterSaleRecord;
import com.dianshang.platform.servicecase.model.CustomerServiceTicket;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.SERVICECASE_MANAGE)
public class ServiceCaseController {

    private final ServiceCaseService serviceCaseService;

    public ServiceCaseController(ServiceCaseService serviceCaseService) {
        this.serviceCaseService = serviceCaseService;
    }

    @GetMapping("/api/tickets")
    public ApiResponse<List<CustomerServiceTicket>> listTickets() {
        return ApiResponse.success(
                serviceCaseService.listTickets(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/tickets/{id}")
    public ApiResponse<CustomerServiceTicket> getTicket(@PathVariable String id) {
        return ApiResponse.success(
                serviceCaseService.getTicket(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/tickets/{id}/reply-suggestion")
    public ApiResponse<CustomerServiceTicket> replySuggestion(@PathVariable String id) {
        return ApiResponse.success(
                serviceCaseService.generateReplySuggestion(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/tickets/{id}/satisfaction")
    public ApiResponse<TicketSatisfactionView> saveSatisfaction(@PathVariable String id,
                                                                @Valid @RequestBody SaveTicketSatisfactionPayload request) {
        return ApiResponse.success(
                serviceCaseService.saveTicketSatisfaction(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/tickets/sla-overview")
    public ApiResponse<TicketSlaOverviewView> getTicketSlaOverview(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                serviceCaseService.getTicketSlaOverview(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/after-sales")
    public ApiResponse<AfterSaleRecord> createAfterSale(@Valid @RequestBody CreateAfterSaleRequest request) {
        return ApiResponse.success(
                serviceCaseService.createAfterSale(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/after-sales/{id}")
    public ApiResponse<AfterSaleRecord> getAfterSale(@PathVariable String id) {
        return ApiResponse.success(
                serviceCaseService.getAfterSale(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/after-sales/{id}/submit-approval")
    public ApiResponse<AfterSaleRecord> submitApproval(@PathVariable String id) {
        return ApiResponse.success(
                serviceCaseService.submitApproval(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id
                ),
                TraceIdHolder.get()
        );
    }
}

record SaveTicketSatisfactionPayload(
        @NotNull(message = "score is required")
        @Min(value = 1, message = "score must be greater than or equal to 1")
        @Max(value = 5, message = "score must be less than or equal to 5")
        Integer score,
        String comment
) {
    SaveTicketSatisfactionRequest toCommand() {
        return new SaveTicketSatisfactionRequest(score, comment);
    }
}
