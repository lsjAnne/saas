package backend.member.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.member.application.MemberService;
import backend.member.application.MemberService.ExportMemberGroupRequest;
import backend.member.application.MemberService.MemberCrmAnalysisView;
import backend.member.application.MemberService.MemberCrmLinkageView;
import backend.member.application.MemberService.MemberCrmProfileView;
import backend.member.application.MemberService.MemberDetailView;
import backend.member.application.MemberService.MemberGroupExportView;
import backend.member.application.MemberService.MemberPageResult;
import backend.member.application.MemberService.MemberQuery;
import backend.member.application.MemberService.MemberSegmentExecutionView;
import backend.member.application.MemberService.MemberSegmentRuleView;
import backend.member.application.MemberService.MemberTagSummaryView;
import backend.member.application.MemberService.MemberTouchTaskView;
import backend.member.application.MemberService.SaveMemberCrmProfileRequest;
import backend.member.application.MemberService.SaveMemberSegmentRuleRequest;
import backend.member.application.MemberService.SaveMemberTagRequest;
import backend.member.application.MemberService.SaveMemberTouchTaskRequest;
import backend.member.application.MemberService.TouchTaskQuery;
import backend.member.model.MemberTag;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.MEMBER_MANAGE)
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/api/members")
    public ApiResponse<MemberPageResult> listMembers(@RequestParam(required = false) String storeId,
                                                     @RequestParam(required = false) String levelCode,
                                                     @RequestParam(required = false) String tagCode,
                                                     @RequestParam(required = false) String lifecycleStage,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(
                memberService.listMembers(TenantAccessSupport.requiredTenantId(),
                        new MemberQuery(storeId, levelCode, tagCode, lifecycleStage, page, pageSize)),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/members/{id}")
    public ApiResponse<MemberDetailView> getMember(@PathVariable String id) {
        return ApiResponse.success(
                memberService.getMember(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/members/{id}/crm-profile")
    public ApiResponse<MemberCrmProfileView> saveCrmProfile(@PathVariable String id,
                                                            @Valid @RequestBody SaveMemberCrmProfilePayload request) {
        return ApiResponse.success(
                memberService.saveCrmProfile(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/members/{id}/crm-linkage")
    public ApiResponse<MemberCrmLinkageView> getCrmLinkage(@PathVariable String id) {
        return ApiResponse.success(
                memberService.getCrmLinkage(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/members/{id}/tags")
    public ApiResponse<MemberTag> addTag(@PathVariable String id, @Valid @RequestBody SaveMemberTagPayload request) {
        return ApiResponse.success(
                memberService.addTag(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @DeleteMapping("/api/members/{id}/tags/{tagId}")
    public ApiResponse<MemberTag> removeTag(@PathVariable String id, @PathVariable String tagId) {
        return ApiResponse.success(
                memberService.removeTag(TenantAccessSupport.requiredTenantId(), id, tagId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/member-tags")
    public ApiResponse<List<MemberTagSummaryView>> listMemberTags(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                memberService.listMemberTags(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/member-segment-rules")
    public ApiResponse<MemberSegmentRuleView> createSegmentRule(@Valid @RequestBody SaveMemberSegmentRulePayload request) {
        return ApiResponse.success(
                memberService.createSegmentRule(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/member-segment-rules/{id}/execute")
    public ApiResponse<MemberSegmentExecutionView> executeSegmentRule(@PathVariable String id) {
        return ApiResponse.success(
                memberService.executeSegmentRule(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/member-crm-analysis")
    public ApiResponse<MemberCrmAnalysisView> getCrmAnalysis(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                memberService.getCrmAnalysis(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/member-touch-tasks")
    public ApiResponse<MemberTouchTaskView> createTouchTask(@Valid @RequestBody SaveMemberTouchTaskPayload request) {
        return ApiResponse.success(
                memberService.createTouchTask(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/member-touch-tasks")
    public ApiResponse<List<MemberTouchTaskView>> listTouchTasks(@RequestParam(required = false) String storeId,
                                                                 @RequestParam(required = false) String memberId,
                                                                 @RequestParam(required = false) String taskType,
                                                                 @RequestParam(required = false) String status) {
        return ApiResponse.success(
                memberService.listTouchTasks(TenantAccessSupport.requiredTenantId(), new TouchTaskQuery(storeId, memberId, taskType, status)),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/member-groups/export")
    public ApiResponse<MemberGroupExportView> exportGroup(@RequestBody(required = false) ExportMemberGroupPayload request) {
        ExportMemberGroupPayload payload = request == null ? new ExportMemberGroupPayload(null, null, null, null) : request;
        return ApiResponse.success(
                memberService.exportMemberGroup(TenantAccessSupport.requiredTenantId(), payload.toCommand()),
                TraceIdHolder.get()
        );
    }
}

record SaveMemberTagPayload(
        @NotBlank(message = "tagCode is required")
        String tagCode,
        @NotBlank(message = "tagName is required")
        String tagName,
        String sourceType
) {
    SaveMemberTagRequest toCommand() {
        return new SaveMemberTagRequest(tagCode, tagName, sourceType);
    }
}

record SaveMemberCrmProfilePayload(
        String customerName,
        String primaryContactName,
        String primaryContactMobile,
        String wechatId,
        String sourceChannel,
        String sourceDetail,
        String customerTier
) {
    SaveMemberCrmProfileRequest toCommand() {
        return new SaveMemberCrmProfileRequest(
                customerName,
                primaryContactName,
                primaryContactMobile,
                wechatId,
                sourceChannel,
                sourceDetail,
                customerTier
        );
    }
}

record SaveMemberSegmentRulePayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "ruleName is required")
        String ruleName,
        String lifecycleStage,
        BigDecimal minTotalPaidAmount,
        String levelCode,
        @NotBlank(message = "tagCode is required")
        String tagCode,
        @NotBlank(message = "tagName is required")
        String tagName
) {
    SaveMemberSegmentRuleRequest toCommand() {
        return new SaveMemberSegmentRuleRequest(
                storeId,
                ruleName,
                lifecycleStage,
                minTotalPaidAmount,
                levelCode,
                tagCode,
                tagName
        );
    }
}

record SaveMemberTouchTaskPayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "memberId is required")
        String memberId,
        @NotBlank(message = "taskType is required")
        String taskType,
        @NotBlank(message = "triggerType is required")
        String triggerType,
        String campaignId,
        @NotBlank(message = "channel is required")
        String channel,
        OffsetDateTime scheduledAt,
        String remark
) {
    SaveMemberTouchTaskRequest toCommand() {
        return new SaveMemberTouchTaskRequest(
                storeId,
                memberId,
                taskType,
                triggerType,
                campaignId,
                channel,
                scheduledAt,
                remark
        );
    }
}

record ExportMemberGroupPayload(
        String storeId,
        String levelCode,
        String tagCode,
        String lifecycleStage
) {
    ExportMemberGroupRequest toCommand() {
        return new ExportMemberGroupRequest(storeId, levelCode, tagCode, lifecycleStage);
    }
}

