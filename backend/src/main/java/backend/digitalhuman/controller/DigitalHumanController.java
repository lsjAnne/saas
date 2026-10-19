package backend.digitalhuman.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.auth.security.RequireTenantPermission;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.digitalhuman.application.DigitalHumanApplicationService;
import backend.digitalhuman.application.DigitalHumanApplicationService.CreateAvatarRequest;
import backend.digitalhuman.application.DigitalHumanApplicationService.CreateScheduleRequest;
import backend.digitalhuman.application.DigitalHumanApplicationService.CreateScriptRequest;
import backend.digitalhuman.application.DigitalHumanApplicationService.DigitalHumanOverviewView;
import backend.digitalhuman.model.DigitalHumanAvatar;
import backend.digitalhuman.model.DigitalHumanSchedule;
import backend.digitalhuman.model.DigitalHumanScript;
import backend.digitalhuman.model.DigitalHumanSession;
import backend.tenant.context.TenantAccessSupport;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.DIGITAL_HUMAN_MANAGE)
public class DigitalHumanController {

    private final DigitalHumanApplicationService digitalHumanApplicationService;

    public DigitalHumanController(DigitalHumanApplicationService digitalHumanApplicationService) {
        this.digitalHumanApplicationService = digitalHumanApplicationService;
    }

    @GetMapping("/api/digital-humans/overview")
    public ApiResponse<DigitalHumanOverviewView> getOverview() {
        return ApiResponse.success(
                digitalHumanApplicationService.getOverview(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    // ---------- 数字人形象 ----------

    @GetMapping("/api/digital-humans/avatars")
    public ApiResponse<List<DigitalHumanAvatar>> listAvatars() {
        return ApiResponse.success(
                digitalHumanApplicationService.listAvatars(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/avatars")
    public ApiResponse<DigitalHumanAvatar> createAvatar(@Valid @RequestBody CreateAvatarRequest request) {
        return ApiResponse.success(
                digitalHumanApplicationService.createAvatar(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/digital-humans/avatars/{id}")
    public ApiResponse<DigitalHumanAvatar> updateAvatar(@PathVariable String id,
                                                        @Valid @RequestBody CreateAvatarRequest request) {
        return ApiResponse.success(
                digitalHumanApplicationService.updateAvatar(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @DeleteMapping("/api/digital-humans/avatars/{id}")
    public ApiResponse<Void> deleteAvatar(@PathVariable String id) {
        digitalHumanApplicationService.deleteAvatar(TenantAccessSupport.requiredTenantId(), id);
        return ApiResponse.success(null, TraceIdHolder.get());
    }

    // ---------- 话术/剧本 ----------

    @GetMapping("/api/digital-humans/scripts")
    public ApiResponse<List<DigitalHumanScript>> listScripts() {
        return ApiResponse.success(
                digitalHumanApplicationService.listScripts(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/scripts")
    public ApiResponse<DigitalHumanScript> createScript(@Valid @RequestBody CreateScriptRequest request) {
        return ApiResponse.success(
                digitalHumanApplicationService.createScript(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/digital-humans/scripts/{id}")
    public ApiResponse<DigitalHumanScript> updateScript(@PathVariable String id,
                                                        @Valid @RequestBody CreateScriptRequest request) {
        return ApiResponse.success(
                digitalHumanApplicationService.updateScript(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    // ---------- 无人排班 ----------

    @GetMapping("/api/digital-humans/schedules")
    public ApiResponse<List<DigitalHumanSchedule>> listSchedules() {
        return ApiResponse.success(
                digitalHumanApplicationService.listSchedules(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/schedules")
    public ApiResponse<DigitalHumanSchedule> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        return ApiResponse.success(
                digitalHumanApplicationService.createSchedule(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/digital-humans/schedules/{id}")
    public ApiResponse<DigitalHumanSchedule> updateSchedule(@PathVariable String id,
                                                            @Valid @RequestBody CreateScheduleRequest request) {
        return ApiResponse.success(
                digitalHumanApplicationService.updateSchedule(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/schedules/{id}/start")
    public ApiResponse<DigitalHumanSession> startSchedule(@PathVariable String id) {
        return ApiResponse.success(
                digitalHumanApplicationService.startSchedule(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/schedules/{id}/stop")
    public ApiResponse<DigitalHumanSession> stopSchedule(@PathVariable String id) {
        return ApiResponse.success(
                digitalHumanApplicationService.stopSchedule(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    // ---------- 开播会话 ----------

    @GetMapping("/api/digital-humans/sessions")
    public ApiResponse<List<DigitalHumanSession>> listSessions() {
        return ApiResponse.success(
                digitalHumanApplicationService.listSessions(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/sessions/{id}/heartbeat")
    public ApiResponse<DigitalHumanSession> heartbeat(@PathVariable String id) {
        return ApiResponse.success(
                digitalHumanApplicationService.heartbeat(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }
}
