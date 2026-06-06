package com.dianshang.platform.fulfillment.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.fulfillment.application.FulfillmentService;
import com.dianshang.platform.fulfillment.application.FulfillmentService.ControlTowerView;
import com.dianshang.platform.fulfillment.application.FulfillmentService.FreightSettlementRecord;
import com.dianshang.platform.fulfillment.application.FulfillmentService.FulfillmentReplayView;
import com.dianshang.platform.fulfillment.application.FulfillmentService.ReverseLogisticsRecord;
import com.dianshang.platform.fulfillment.application.FulfillmentService.TmsCarrier;
import com.dianshang.platform.fulfillment.application.FulfillmentService.TmsShipment;
import com.dianshang.platform.fulfillment.application.FulfillmentService.TmsTrackingEvent;
import com.dianshang.platform.fulfillment.dto.CreateLogisticsRecordRequest;
import com.dianshang.platform.fulfillment.model.FulfillmentTask;
import com.dianshang.platform.fulfillment.model.LogisticsRecord;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.FULFILLMENT_MANAGE)
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping("/api/fulfillment-tasks")
    public ApiResponse<List<FulfillmentTask>> listTasks() {
        return ApiResponse.success(
                fulfillmentService.listTasks(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/fulfillment-tasks/{id}")
    public ApiResponse<FulfillmentTask> getTask(@PathVariable String id) {
        return ApiResponse.success(
                fulfillmentService.getTask(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/fulfillment-tasks/{id}/confirm")
    public ApiResponse<FulfillmentTask> confirmTask(@PathVariable String id) {
        return ApiResponse.success(
                fulfillmentService.confirmTask(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/fulfillment-tasks/{id}/retry")
    public ApiResponse<FulfillmentTask> retryTask(@PathVariable String id) {
        return ApiResponse.success(
                fulfillmentService.retryTask(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/fulfillment-tasks/{id}/exception-replay")
    public ApiResponse<FulfillmentReplayView> replayException(@PathVariable String id) {
        return ApiResponse.success(
                fulfillmentService.replayException(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/fulfillment-tasks/{id}/logistics-records")
    public ApiResponse<List<LogisticsRecord>> listLogisticsRecords(@PathVariable String id) {
        return ApiResponse.success(
                fulfillmentService.listLogisticsRecords(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/fulfillment-tasks/{id}/logistics-records")
    public ApiResponse<LogisticsRecord> createLogisticsRecord(@PathVariable String id,
                                                              @Valid @RequestBody CreateLogisticsRecordRequest request) {
        return ApiResponse.success(
                fulfillmentService.createLogisticsRecord(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/tms/carriers")
    public ApiResponse<TmsCarrier> createCarrier(@Valid @RequestBody CreateTmsCarrierRequest request) {
        return ApiResponse.success(
                fulfillmentService.createCarrier(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/tms/shipments")
    public ApiResponse<TmsShipment> createShipment(@Valid @RequestBody CreateTmsShipmentRequest request) {
        return ApiResponse.success(
                fulfillmentService.createShipment(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/tms/shipments/{id}/tracking-events")
    public ApiResponse<TmsTrackingEvent> createTrackingEvent(@PathVariable String id,
                                                             @Valid @RequestBody CreateTmsTrackingEventRequest request) {
        return ApiResponse.success(
                fulfillmentService.createTrackingEvent(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id,
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/tms/freight-settlements")
    public ApiResponse<FreightSettlementRecord> createFreightSettlement(
            @Valid @RequestBody CreateFreightSettlementRequest request
    ) {
        return ApiResponse.success(
                fulfillmentService.createFreightSettlement(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/tms/shipments/{id}/sign-off")
    public ApiResponse<TmsShipment> signOffShipment(@PathVariable String id,
                                                    @Valid @RequestBody SignOffShipmentRequest request) {
        return ApiResponse.success(
                fulfillmentService.signOffShipment(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id,
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/tms/reverse-logistics")
    public ApiResponse<ReverseLogisticsRecord> createReverseLogistics(
            @Valid @RequestBody CreateReverseLogisticsRequest request
    ) {
        return ApiResponse.success(
                fulfillmentService.createReverseLogistics(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/tms/control-tower")
    public ApiResponse<ControlTowerView> getControlTower() {
        return ApiResponse.success(
                fulfillmentService.getControlTower(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    public record CreateTmsCarrierRequest(
            @NotBlank(message = "storeId is required")
            String storeId,
            @NotBlank(message = "carrierName is required")
            String carrierName,
            @NotBlank(message = "carrierCode is required")
            String carrierCode,
            @NotBlank(message = "channelType is required")
            String channelType,
            @NotBlank(message = "serviceScope is required")
            String serviceScope
    ) {
        FulfillmentService.CreateTmsCarrierCommand toCommand() {
            return new FulfillmentService.CreateTmsCarrierCommand(
                    storeId,
                    carrierName,
                    carrierCode,
                    channelType,
                    serviceScope
            );
        }
    }

    public record CreateTmsShipmentRequest(
            @NotBlank(message = "fulfillmentTaskId is required")
            String fulfillmentTaskId,
            @NotBlank(message = "carrierId is required")
            String carrierId,
            @NotBlank(message = "shippingMode is required")
            String shippingMode,
            @NotNull(message = "freightAmount is required")
            @DecimalMin(value = "0.01", message = "freightAmount must be greater than 0")
            BigDecimal freightAmount,
            @NotBlank(message = "originCity is required")
            String originCity,
            @NotBlank(message = "destinationCity is required")
            String destinationCity
    ) {
        FulfillmentService.CreateTmsShipmentCommand toCommand() {
            return new FulfillmentService.CreateTmsShipmentCommand(
                    fulfillmentTaskId,
                    carrierId,
                    shippingMode,
                    freightAmount,
                    originCity,
                    destinationCity
            );
        }
    }

    public record CreateTmsTrackingEventRequest(
            @NotBlank(message = "trackingStatus is required")
            String trackingStatus,
            @NotBlank(message = "locationText is required")
            String locationText,
            String remark
    ) {
        FulfillmentService.CreateTmsTrackingEventCommand toCommand() {
            return new FulfillmentService.CreateTmsTrackingEventCommand(
                    trackingStatus,
                    locationText,
                    remark
            );
        }
    }

    public record CreateFreightSettlementRequest(
            @NotBlank(message = "shipmentId is required")
            String shipmentId,
            @NotBlank(message = "settleMode is required")
            String settleMode,
            @NotBlank(message = "costType is required")
            String costType,
            @NotNull(message = "billableWeight is required")
            @DecimalMin(value = "0.01", message = "billableWeight must be greater than 0")
            BigDecimal billableWeight,
            @NotNull(message = "freightAmount is required")
            @DecimalMin(value = "0.01", message = "freightAmount must be greater than 0")
            BigDecimal freightAmount
    ) {
        FulfillmentService.CreateFreightSettlementCommand toCommand() {
            return new FulfillmentService.CreateFreightSettlementCommand(
                    shipmentId,
                    settleMode,
                    costType,
                    billableWeight,
                    freightAmount
            );
        }
    }

    public record SignOffShipmentRequest(
            @NotBlank(message = "signStatus is required")
            String signStatus,
            @NotBlank(message = "proofType is required")
            String proofType,
            String remark
    ) {
        FulfillmentService.SignOffShipmentCommand toCommand() {
            return new FulfillmentService.SignOffShipmentCommand(signStatus, proofType, remark);
        }
    }

    public record CreateReverseLogisticsRequest(
            @NotBlank(message = "orderId is required")
            String orderId,
            @NotBlank(message = "shipmentId is required")
            String shipmentId,
            @NotBlank(message = "carrierId is required")
            String carrierId,
            @NotBlank(message = "reverseType is required")
            String reverseType,
            String remark
    ) {
        FulfillmentService.CreateReverseLogisticsCommand toCommand() {
            return new FulfillmentService.CreateReverseLogisticsCommand(
                    orderId,
                    shipmentId,
                    carrierId,
                    reverseType,
                    remark
            );
        }
    }
}
