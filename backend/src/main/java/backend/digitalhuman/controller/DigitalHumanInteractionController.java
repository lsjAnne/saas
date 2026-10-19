package backend.digitalhuman.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.auth.security.RequireTenantPermission;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.digitalhuman.application.DigitalHumanInteractionService;
import backend.digitalhuman.application.DigitalHumanInteractionService.CreateFaqRequest;
import backend.digitalhuman.application.DigitalHumanInteractionService.InteractionOverviewView;
import backend.digitalhuman.application.DigitalHumanInteractionService.InteractionReplyView;
import backend.digitalhuman.application.DigitalHumanInteractionService.SimulateBarrageRequest;
import backend.digitalhuman.application.DigitalHumanInteractionService.SubmitBarrageRequest;
import backend.digitalhuman.model.DigitalHumanBarrage;
import backend.digitalhuman.model.DigitalHumanFaq;
import backend.digitalhuman.model.DigitalHumanInteraction;
import backend.tenant.context.TenantAccessSupport;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.DIGITAL_HUMAN_MANAGE)
public class DigitalHumanInteractionController {

    private final DigitalHumanInteractionService interactionService;

    public DigitalHumanInteractionController(DigitalHumanInteractionService interactionService) {
        this.interactionService = interactionService;
    }

    // ---------- 问答知识库 ----------

    @GetMapping("/api/digital-humans/faqs")
    public ApiResponse<List<DigitalHumanFaq>> listFaqs() {
        return ApiResponse.success(
                interactionService.listFaqs(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/faqs")
    public ApiResponse<DigitalHumanFaq> createFaq(@Valid @RequestBody CreateFaqRequest request) {
        return ApiResponse.success(
                interactionService.createFaq(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/digital-humans/faqs/{id}")
    public ApiResponse<DigitalHumanFaq> updateFaq(@PathVariable String id,
                                                  @Valid @RequestBody CreateFaqRequest request) {
        return ApiResponse.success(
                interactionService.updateFaq(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @DeleteMapping("/api/digital-humans/faqs/{id}")
    public ApiResponse<Void> deleteFaq(@PathVariable String id) {
        interactionService.deleteFaq(TenantAccessSupport.requiredTenantId(), id);
        return ApiResponse.success(null, TraceIdHolder.get());
    }

    // ---------- 弹幕问答 ----------

    @GetMapping("/api/digital-humans/barrages")
    public ApiResponse<List<DigitalHumanBarrage>> listBarrages(
            @RequestParam(required = false) String sessionId) {
        return ApiResponse.success(
                interactionService.listBarrages(TenantAccessSupport.requiredTenantId(), sessionId),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/barrages")
    public ApiResponse<InteractionReplyView> submitBarrage(@Valid @RequestBody SubmitBarrageRequest request) {
        return ApiResponse.success(
                interactionService.submitBarrage(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/barrages/simulate")
    public ApiResponse<List<InteractionReplyView>> simulateBarrages(@Valid @RequestBody SimulateBarrageRequest request) {
        return ApiResponse.success(
                interactionService.simulateBarrages(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/digital-humans/sessions/{id}/interactions")
    public ApiResponse<List<DigitalHumanInteraction>> listInteractions(@PathVariable String id) {
        return ApiResponse.success(
                interactionService.listInteractions(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/digital-humans/interactions/recent")
    public ApiResponse<List<DigitalHumanInteraction>> listRecentInteractions(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(
                interactionService.listRecentInteractions(TenantAccessSupport.requiredTenantId(), limit),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/digital-humans/interaction-overview")
    public ApiResponse<InteractionOverviewView> interactionOverview() {
        return ApiResponse.success(
                interactionService.interactionOverview(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }
}
