package backend.digitalhuman.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.auth.security.RequireTenantPermission;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.digitalhuman.application.DigitalHumanLearningService;
import backend.digitalhuman.model.DigitalHumanLearningItem;
import backend.tenant.context.TenantAccessSupport;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数字人学习记忆接口：查看数字人记住了什么、确认/忽略未答问题、查看成长指标。
 */
@RestController
@RequireTenantPermission(AuthPermissionCodes.DIGITAL_HUMAN_MANAGE)
public class DigitalHumanLearningController {

    private final DigitalHumanLearningService learningService;

    public DigitalHumanLearningController(DigitalHumanLearningService learningService) {
        this.learningService = learningService;
    }

    @GetMapping("/api/digital-humans/learning/items")
    public ApiResponse<List<DigitalHumanLearningItem>> listLearningItems() {
        return ApiResponse.success(
                learningService.listItems(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/learning/items/{id}/confirm")
    public ApiResponse<DigitalHumanLearningItem> confirmLearningItem(@PathVariable String id) {
        return ApiResponse.success(
                learningService.confirm(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/digital-humans/learning/items/{id}/ignore")
    public ApiResponse<DigitalHumanLearningItem> ignoreLearningItem(@PathVariable String id) {
        return ApiResponse.success(
                learningService.ignore(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/digital-humans/learning/growth")
    public ApiResponse<Map<String, Object>> learningGrowth() {
        return ApiResponse.success(
                learningService.growth(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }
}
