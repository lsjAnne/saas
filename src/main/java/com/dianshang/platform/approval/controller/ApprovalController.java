package com.dianshang.platform.approval.controller;

import com.dianshang.platform.approval.application.ApprovalApplicationService;
import com.dianshang.platform.approval.application.ApprovalService.ApprovalStageDefinition;
import com.dianshang.platform.approval.application.ApprovalService.ApprovalTemplateView;
import com.dianshang.platform.approval.application.ApprovalService.CreateApprovalRequest;
import com.dianshang.platform.approval.application.ApprovalService.CreateApprovalTemplateRequest;
import com.dianshang.platform.approval.application.ApprovalService.CreateGovernanceApprovalRequest;
import com.dianshang.platform.approval.application.ApprovalService.GovernanceApprovalRequestView;
import com.dianshang.platform.approval.application.ApprovalService.TransferApprovalRequest;
import com.dianshang.platform.approval.model.ApprovalInstance;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.APPROVAL_MANAGE)
public class ApprovalController {

    private final ApprovalApplicationService approvalApplicationService;

    public ApprovalController(ApprovalApplicationService approvalApplicationService) {
        this.approvalApplicationService = approvalApplicationService;
    }

    @GetMapping("/api/approvals")
    public ApiResponse<List<ApprovalInstance>> listApprovals() {
        return ApiResponse.success(
                approvalApplicationService.listApprovals(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/approvals")
    public ApiResponse<ApprovalInstance> createApproval(@Valid @RequestBody SaveApprovalRequest request) {
        return ApiResponse.success(
                approvalApplicationService.createApproval(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/approvals/{id}")
    public ApiResponse<ApprovalInstance> getApproval(@PathVariable String id) {
        return ApiResponse.success(
                approvalApplicationService.getApproval(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/approvals/{id}/approve")
    public ApiResponse<ApprovalInstance> approve(@PathVariable String id,
                                                 @RequestBody(required = false) ApprovalActionRequest request) {
        return ApiResponse.success(
                approvalApplicationService.approve(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id,
                        request == null ? null : request.remark()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/approvals/{id}/reject")
    public ApiResponse<ApprovalInstance> reject(@PathVariable String id,
                                                @RequestBody(required = false) ApprovalActionRequest request) {
        return ApiResponse.success(
                approvalApplicationService.reject(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id,
                        request == null ? null : request.remark()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/approvals/{id}/transfer")
    public ApiResponse<ApprovalInstance> transfer(@PathVariable String id,
                                                  @Valid @RequestBody ApprovalTransferRequest request) {
        return ApiResponse.success(
                approvalApplicationService.transfer(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id,
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/approval-templates")
    public ApiResponse<List<ApprovalTemplateView>> listApprovalTemplates() {
        return ApiResponse.success(
                approvalApplicationService.listApprovalTemplates(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/approval-templates")
    public ApiResponse<ApprovalTemplateView> createApprovalTemplate(@Valid @RequestBody SaveApprovalTemplateRequest request) {
        return ApiResponse.success(
                approvalApplicationService.createApprovalTemplate(
                        TenantAccessSupport.requiredTenantId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/governance-approval-requests")
    public ApiResponse<List<GovernanceApprovalRequestView>> listGovernanceApprovalRequests() {
        return ApiResponse.success(
                approvalApplicationService.listGovernanceApprovalRequests(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/governance-approval-requests/{id}")
    public ApiResponse<GovernanceApprovalRequestView> getGovernanceApprovalRequest(@PathVariable String id) {
        return ApiResponse.success(
                approvalApplicationService.getGovernanceApprovalRequest(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/governance-approval-requests")
    public ApiResponse<GovernanceApprovalRequestView> createGovernanceApprovalRequest(
            @Valid @RequestBody SaveGovernanceApprovalRequest request) {
        return ApiResponse.success(
                approvalApplicationService.createGovernanceApprovalRequest(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }
}

record SaveApprovalRequest(
        @NotBlank(message = "approvalType is required")
        String approvalType,
        @NotBlank(message = "relatedType is required")
        String relatedType,
        @NotBlank(message = "relatedId is required")
        String relatedId,
        String currentHandlerId,
        String remark
) {
    CreateApprovalRequest toCommand() {
        return new CreateApprovalRequest(approvalType, relatedType, relatedId, currentHandlerId, remark);
    }
}

record ApprovalActionRequest(
        String remark
) {
}

record ApprovalTransferRequest(
        @NotBlank(message = "currentHandlerId is required")
        String currentHandlerId,
        String remark
) {
    TransferApprovalRequest toCommand() {
        return new TransferApprovalRequest(currentHandlerId, remark);
    }
}

record SaveApprovalTemplateRequest(
        @NotBlank(message = "templateCode is required")
        String templateCode,
        @NotBlank(message = "templateName is required")
        String templateName,
        @NotBlank(message = "approvalType is required")
        String approvalType,
        Boolean enabled,
        @NotEmpty(message = "stages is required")
        List<SaveApprovalStageRequest> stages
) {
    CreateApprovalTemplateRequest toCommand() {
        return new CreateApprovalTemplateRequest(
                templateCode,
                templateName,
                approvalType,
                enabled,
                stages.stream().map(SaveApprovalStageRequest::toCommand).toList()
        );
    }
}

record SaveApprovalStageRequest(
        @NotBlank(message = "stageCode is required")
        String stageCode,
        @NotBlank(message = "handlerId is required")
        String handlerId,
        @NotNull(message = "stageOrder is required")
        Integer stageOrder
) {
    ApprovalStageDefinition toCommand() {
        return new ApprovalStageDefinition(stageCode, handlerId, stageOrder);
    }
}

record SaveGovernanceApprovalRequest(
        @NotBlank(message = "approvalType is required")
        String approvalType,
        @NotBlank(message = "templateCode is required")
        String templateCode,
        @NotBlank(message = "documentNo is required")
        String documentNo,
        @NotBlank(message = "subject is required")
        String subject,
        @NotNull(message = "amount is required")
        BigDecimal amount,
        @NotBlank(message = "counterparty is required")
        String counterparty,
        String remark
) {
    CreateGovernanceApprovalRequest toCommand() {
        return new CreateGovernanceApprovalRequest(
                approvalType,
                templateCode,
                documentNo,
                subject,
                amount,
                counterparty,
                remark
        );
    }
}
