package backend.exceptioncenter.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.auth.security.AuthPermissionCodes;
import backend.exceptioncenter.application.ExceptionService;
import backend.exceptioncenter.dto.UpdateExceptionTaskRequest;
import backend.exceptioncenter.model.ExceptionTask;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exceptions")
@RequireTenantPermission(AuthPermissionCodes.EXCEPTION_MANAGE)
public class ExceptionController {

    private final ExceptionService exceptionService;

    public ExceptionController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    @GetMapping
    public ApiResponse<List<ExceptionTask>> listExceptions() {
        return ApiResponse.success(
                exceptionService.listExceptions(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<ExceptionTask> getException(@PathVariable String id) {
        return ApiResponse.success(
                exceptionService.getException(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{id}/process")
    public ApiResponse<ExceptionTask> process(@PathVariable String id,
                                              @RequestBody(required = false) UpdateExceptionTaskRequest request) {
        return ApiResponse.success(
                exceptionService.process(TenantAccessSupport.requiredTenantId(), id, defaultRequest(request)),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{id}/ignore")
    public ApiResponse<ExceptionTask> ignore(@PathVariable String id,
                                             @RequestBody(required = false) UpdateExceptionTaskRequest request) {
        return ApiResponse.success(
                exceptionService.ignore(TenantAccessSupport.requiredTenantId(), id, defaultRequest(request)),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{id}/escalate")
    public ApiResponse<ExceptionTask> escalate(@PathVariable String id,
                                               @RequestBody(required = false) UpdateExceptionTaskRequest request) {
        return ApiResponse.success(
                exceptionService.escalate(TenantAccessSupport.requiredTenantId(), id, defaultRequest(request)),
                TraceIdHolder.get()
        );
    }

    private UpdateExceptionTaskRequest defaultRequest(UpdateExceptionTaskRequest request) {
        return request == null ? new UpdateExceptionTaskRequest(null, null, null) : request;
    }
}

