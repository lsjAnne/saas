package backend.order.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.order.application.OrderApplicationService;
import backend.order.application.OrderApplicationService.OmsWorkbenchView;
import backend.order.application.OrderApplicationService.OrderAuditReviewView;
import backend.order.application.OrderApplicationService.OrderMergeView;
import backend.order.application.OrderApplicationService.OrderOrchestrationView;
import backend.order.application.OrderApplicationService.OrderReverseStatusView;
import backend.order.application.OrderApplicationService.OrderRoutePlanView;
import backend.order.application.OrderApplicationService.OrderSplitChildView;
import backend.order.application.OrderApplicationService.OrderSplitView;
import backend.order.application.OrderApplicationService.SaveOrderMergeRequest;
import backend.order.application.OrderApplicationService.SaveOrderReverseStatusRequest;
import backend.order.application.OrderApplicationService.SaveOrderRoutePlanRequest;
import backend.order.application.OrderApplicationService.SaveOrderSplitRequest;
import backend.order.application.OrderApplicationService.StandardizedOrderView;
import backend.order.dto.CreateOrderSyncRequest;
import backend.order.dto.OrderDetailView;
import backend.order.dto.OrderSyncResult;
import backend.order.model.OrderMain;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.ORDER_MANAGE)
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @GetMapping("/api/orders")
    public ApiResponse<List<OrderMain>> listOrders() {
        return ApiResponse.success(
                orderApplicationService.listOrders(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/orders/oms-standardized")
    public ApiResponse<List<StandardizedOrderView>> listStandardizedOrders() {
        return ApiResponse.success(
                orderApplicationService.listStandardizedOrders(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/orders/oms-workbench")
    public ApiResponse<OmsWorkbenchView> getOmsWorkbench() {
        return ApiResponse.success(
                orderApplicationService.getOmsWorkbench(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/orders/merge")
    public ApiResponse<OrderMergeView> mergeOrders(@Valid @RequestBody SaveOrderMergePayload request) {
        return ApiResponse.success(
                orderApplicationService.mergeOrders(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/orders/{id}")
    public ApiResponse<OrderDetailView> getOrder(@PathVariable String id) {
        return ApiResponse.success(
                orderApplicationService.getOrder(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/orders/{id}/orchestration-view")
    public ApiResponse<OrderOrchestrationView> getOrchestrationView(@PathVariable String id) {
        return ApiResponse.success(
                orderApplicationService.getOrchestrationView(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/orders/{id}/audit-review")
    public ApiResponse<OrderAuditReviewView> auditOrder(@PathVariable String id) {
        return ApiResponse.success(
                orderApplicationService.auditOrder(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/orders/{id}/split")
    public ApiResponse<OrderSplitView> splitOrder(@PathVariable String id,
                                                  @Valid @RequestBody SaveOrderSplitPayload request) {
        return ApiResponse.success(
                orderApplicationService.splitOrder(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/orders/{id}/route-plan")
    public ApiResponse<OrderRoutePlanView> planRoute(@PathVariable String id,
                                                     @Valid @RequestBody SaveOrderRoutePlanPayload request) {
        return ApiResponse.success(
                orderApplicationService.planRoute(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/orders/{id}/reverse-status")
    public ApiResponse<OrderReverseStatusView> updateReverseStatus(@PathVariable String id,
                                                                   @Valid @RequestBody SaveOrderReverseStatusPayload request) {
        return ApiResponse.success(
                orderApplicationService.updateReverseStatus(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/orders/sync")
    public ApiResponse<OrderSyncResult> syncOrder(@Valid @RequestBody CreateOrderSyncRequest request) {
        return ApiResponse.success(
                orderApplicationService.syncOrder(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }
}

record SaveOrderSplitPayload(
        String remark,
        @NotEmpty(message = "children are required")
        List<SaveOrderSplitChildPayload> children
) {
    SaveOrderSplitRequest toCommand() {
        return new SaveOrderSplitRequest(
                remark,
                children.stream()
                        .map(item -> new OrderSplitChildView(null, item.childLabel(), item.quantity(), item.orderType()))
                        .toList()
        );
    }
}

record SaveOrderSplitChildPayload(
        @NotBlank(message = "childLabel is required")
        String childLabel,
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than 0")
        Integer quantity,
        @NotBlank(message = "orderType is required")
        String orderType
) {
}

record SaveOrderMergePayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotEmpty(message = "orderIds are required")
        List<String> orderIds,
        @NotBlank(message = "mergedOrderType is required")
        String mergedOrderType,
        String remark
) {
    SaveOrderMergeRequest toCommand() {
        return new SaveOrderMergeRequest(storeId, orderIds, mergedOrderType, remark);
    }
}

record SaveOrderRoutePlanPayload(
        List<String> warehouseCandidates,
        String shippingStrategy
) {
    SaveOrderRoutePlanRequest toCommand() {
        return new SaveOrderRoutePlanRequest(warehouseCandidates, shippingStrategy);
    }
}

record SaveOrderReverseStatusPayload(
        @NotBlank(message = "reverseStatus is required")
        String reverseStatus,
        String remark
) {
    SaveOrderReverseStatusRequest toCommand() {
        return new SaveOrderReverseStatusRequest(reverseStatus, remark);
    }
}

