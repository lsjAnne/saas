package com.dianshang.platform.qa.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.qa.application.QaApplicationService;
import com.dianshang.platform.qa.application.QaService.SendConversationMessageRequest;
import com.dianshang.platform.qa.application.QaService.TransferConversationManualRequest;
import com.dianshang.platform.qa.dto.CreateFaqKnowledgeRequest;
import com.dianshang.platform.qa.dto.ReplySuggestionView;
import com.dianshang.platform.qa.model.ConversationMessage;
import com.dianshang.platform.qa.model.CustomerConversation;
import com.dianshang.platform.qa.model.FaqKnowledge;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.QA_MANAGE)
public class QaController {

    private final QaApplicationService qaApplicationService;

    public QaController(QaApplicationService qaApplicationService) {
        this.qaApplicationService = qaApplicationService;
    }

    @GetMapping("/api/conversations")
    public ApiResponse<List<CustomerConversation>> listConversations() {
        return ApiResponse.success(
                qaApplicationService.listConversations(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/conversations/{id}")
    public ApiResponse<CustomerConversation> getConversation(@PathVariable String id) {
        return ApiResponse.success(
                qaApplicationService.getConversation(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/conversations/{id}/messages")
    public ApiResponse<List<ConversationMessage>> listMessages(@PathVariable String id) {
        return ApiResponse.success(
                qaApplicationService.listMessages(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/conversations/{id}/reply-suggestion")
    public ApiResponse<ReplySuggestionView> replySuggestion(@PathVariable String id) {
        return ApiResponse.success(
                qaApplicationService.generateReplySuggestion(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/conversations/{id}/send")
    public ApiResponse<ConversationMessage> sendMessage(@PathVariable String id,
                                                        @Valid @RequestBody SendConversationMessageRequest request) {
        return ApiResponse.success(
                qaApplicationService.sendMessage(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/conversations/{id}/transfer-manual")
    public ApiResponse<CustomerConversation> transferManual(@PathVariable String id,
                                                            @Valid @RequestBody TransferConversationManualRequest request) {
        return ApiResponse.success(
                qaApplicationService.transferManual(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/faq-knowledge")
    public ApiResponse<List<FaqKnowledge>> listFaqKnowledge() {
        return ApiResponse.success(
                qaApplicationService.listFaqKnowledge(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/faq-knowledge")
    public ApiResponse<FaqKnowledge> createFaqKnowledge(@Valid @RequestBody CreateFaqKnowledgeRequest request) {
        return ApiResponse.success(
                qaApplicationService.createFaqKnowledge(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/faq-knowledge/{id}")
    public ApiResponse<FaqKnowledge> updateFaqKnowledge(@PathVariable String id,
                                                        @Valid @RequestBody CreateFaqKnowledgeRequest request) {
        return ApiResponse.success(
                qaApplicationService.updateFaqKnowledge(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }
}
