package backend.qa.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.auth.security.AuthPermissionCodes;
import backend.qa.application.QaApplicationService;
import backend.qa.application.QaService.SendConversationMessageRequest;
import backend.qa.application.QaService.TransferConversationManualRequest;
import backend.qa.dto.CreateFaqKnowledgeRequest;
import backend.qa.dto.ReplySuggestionView;
import backend.qa.model.ConversationMessage;
import backend.qa.model.CustomerConversation;
import backend.qa.model.FaqKnowledge;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
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

