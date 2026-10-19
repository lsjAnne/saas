package backend.live.barrage.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.auth.security.RequireTenantPermission;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.live.barrage.application.BarrageConnectorManager;
import backend.live.barrage.model.BarrageConnectionStatus;
import backend.tenant.context.TenantAccessSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 弹幕源接入管理：查看/启动/停止各直播平台的弹幕拉流。
 */
@RestController
@RequireTenantPermission(AuthPermissionCodes.DIGITAL_HUMAN_MANAGE)
public class BarrageConnectionController {

    private final BarrageConnectorManager manager;

    public BarrageConnectionController(BarrageConnectorManager manager) {
        this.manager = manager;
    }

    /** 全部平台弹幕源状态 */
    @GetMapping("/api/live/barrage-connections")
    public ApiResponse<List<BarrageConnectionStatus>> listConnections() {
        return ApiResponse.success(manager.listStatus(), TraceIdHolder.get());
    }

    /** 当前生效的弹幕源（mock/douyin/kuaishou/wechat_channels/none） */
    @GetMapping("/api/live/barrage-connections/active")
    public ApiResponse<String> activeConnection() {
        return ApiResponse.success(manager.activeSource(), TraceIdHolder.get());
    }

    /** 启动指定平台弹幕拉流 */
    @PostMapping("/api/live/barrage-connections/{platformType}/start")
    public ApiResponse<List<BarrageConnectionStatus>> start(@PathVariable String platformType) {
        manager.start(
                TenantAccessSupport.requiredTenantId(),
                TenantAccessSupport.requiredOrganizationId(),
                platformType
        );
        return ApiResponse.success(manager.listStatus(), TraceIdHolder.get());
    }

    /** 停止指定平台弹幕拉流 */
    @PostMapping("/api/live/barrage-connections/{platformType}/stop")
    public ApiResponse<List<BarrageConnectionStatus>> stop(@PathVariable String platformType) {
        manager.stop(TenantAccessSupport.requiredOrganizationId(), platformType);
        return ApiResponse.success(manager.listStatus(), TraceIdHolder.get());
    }
}
