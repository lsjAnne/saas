package backend.fulfillment.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.exceptioncenter.application.ExceptionService;
import backend.fulfillment.domain.repository.FulfillmentTaskRepository;
import backend.fulfillment.domain.repository.LogisticsRecordRepository;
import backend.fulfillment.dto.CreateLogisticsRecordRequest;
import backend.fulfillment.model.FulfillmentTask;
import backend.fulfillment.model.LogisticsRecord;
import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderMain;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FulfillmentService {

    private static final List<String> RETRYABLE_STATUSES = List.of("failed", "manual_required");

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final LogisticsRecordRepository logisticsRecordRepository;
    private final OrderRepository orderRepository;
    private final ExceptionService exceptionService;
    private final ExternalRoutingAdapter externalRoutingAdapter;
    private static final Map<String, TmsCarrier> TMS_CARRIER_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong TMS_CARRIER_SEQUENCE = new AtomicLong();
    private static final Map<String, TmsShipment> TMS_SHIPMENT_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong TMS_SHIPMENT_SEQUENCE = new AtomicLong();
    private static final Map<String, TmsTrackingEvent> TMS_TRACKING_EVENT_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong TMS_TRACKING_EVENT_SEQUENCE = new AtomicLong();
    private static final Map<String, FreightSettlementRecord> TMS_FREIGHT_SETTLEMENT_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong TMS_FREIGHT_SETTLEMENT_SEQUENCE = new AtomicLong();
    private static final Map<String, ReverseLogisticsRecord> TMS_REVERSE_LOGISTICS_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong TMS_REVERSE_LOGISTICS_SEQUENCE = new AtomicLong();

    public FulfillmentService(AuditLogService auditLogService,
                              StoreRepository storeRepository,
                              FulfillmentTaskRepository fulfillmentTaskRepository,
                              LogisticsRecordRepository logisticsRecordRepository,
                              OrderRepository orderRepository,
                              ExceptionService exceptionService,
                              ExternalRoutingAdapter externalRoutingAdapter) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.fulfillmentTaskRepository = fulfillmentTaskRepository;
        this.logisticsRecordRepository = logisticsRecordRepository;
        this.orderRepository = orderRepository;
        this.exceptionService = exceptionService;
        this.externalRoutingAdapter = externalRoutingAdapter;
    }

    public List<FulfillmentTask> listTasks(String tenantId) {
        return fulfillmentTaskRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public FulfillmentTask getTask(String tenantId, String fulfillmentTaskId) {
        FulfillmentTask task = fulfillmentTaskRepository.findByFulfillmentTaskId(fulfillmentTaskId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, task.storeId());
        return task;
    }

    public FulfillmentTask confirmTask(String tenantId, String fulfillmentTaskId) {
        FulfillmentTask current = getTask(tenantId, fulfillmentTaskId);
        if (!"pending_confirm".equals(current.status())) {
            throw new BusinessException("1004", "current status does not allow confirm", HttpStatus.BAD_REQUEST);
        }
        FulfillmentTask updated = fulfillmentTaskRepository.save(current.withStatus("pending_execute", null));
        auditLogService.recordForTenant(tenantId, "CONFIRM_FULFILLMENT_TASK", "fulfillment_task", fulfillmentTaskId);
        return updated;
    }

    public FulfillmentTask retryTask(String tenantId, String fulfillmentTaskId) {
        FulfillmentTask current = getTask(tenantId, fulfillmentTaskId);
        if (!RETRYABLE_STATUSES.contains(current.status())) {
            throw new BusinessException("1004", "current status does not allow retry", HttpStatus.BAD_REQUEST);
        }
        FulfillmentTask updated = fulfillmentTaskRepository.save(current.withRetry("pending_execute"));
        auditLogService.recordForTenant(tenantId, "RETRY_FULFILLMENT_TASK", "fulfillment_task", fulfillmentTaskId);
        return updated;
    }

    public FulfillmentReplayView replayException(String tenantId, String fulfillmentTaskId) {
        FulfillmentTask updated = retryTask(tenantId, fulfillmentTaskId);
        auditLogService.recordForTenant(tenantId, "REPLAY_FULFILLMENT_EXCEPTION", "fulfillment_task", fulfillmentTaskId);
        return new FulfillmentReplayView(
                updated.fulfillmentTaskId(),
                updated.status(),
                updated.retryCount() == null ? 0 : updated.retryCount()
        );
    }

    public List<LogisticsRecord> listLogisticsRecords(String tenantId, String fulfillmentTaskId) {
        getTask(tenantId, fulfillmentTaskId);
        return logisticsRecordRepository.findByFulfillmentTaskId(fulfillmentTaskId);
    }

    public LogisticsRecord createLogisticsRecord(String tenantId,
                                                 String fulfillmentTaskId,
                                                 CreateLogisticsRecordRequest request) {
        FulfillmentTask current = getTask(tenantId, fulfillmentTaskId);
        if ("pending_confirm".equals(current.status()) || "cancelled".equals(current.status())) {
            throw new BusinessException("1004", "current status does not allow logistics callback", HttpStatus.BAD_REQUEST);
        }

        LogisticsRecord logisticsRecord = logisticsRecordRepository.save(new LogisticsRecord(
                null,
                fulfillmentTaskId,
                request.trackingNumber(),
                request.logisticsCompany(),
                request.logisticsStatus(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        ));

        if ("exception".equalsIgnoreCase(request.logisticsStatus())) {
            fulfillmentTaskRepository.save(current.withStatus("manual_required", "logistics exception callback"));
            exceptionService.createAutoException(
                    tenantId,
                    current.storeId(),
                    "fulfillment_task",
                    current.fulfillmentTaskId(),
                    "logistics_exception",
                    "P1",
                    "鐗╂祦鍥炰紶寮傚父锛岄渶浜哄伐鍏滃簳"
            );
        } else {
            fulfillmentTaskRepository.save(current.withStatus("success", null));
            OrderMain order = requireOrder(current.orderId());
            orderRepository.save(order.withStatus("shipped", request.logisticsStatus()));
        }

        auditLogService.recordForTenant(tenantId, "CREATE_LOGISTICS_RECORD", "logistics_record", logisticsRecord.logisticsRecordId());
        return logisticsRecord;
    }

    public TmsCarrier createCarrier(String tenantId,
                                    String operatorId,
                                    CreateTmsCarrierCommand command) {
        requireOwnedStore(tenantId, command.storeId());
        OffsetDateTime now = OffsetDateTime.now();
        String sequenceValue = String.valueOf(TMS_CARRIER_SEQUENCE.incrementAndGet());
        TmsCarrier carrier = new TmsCarrier(
                "tms-carrier-" + sequenceValue,
                command.storeId(),
                command.carrierName(),
                command.carrierCode(),
                command.channelType(),
                command.serviceScope(),
                "active",
                operatorId,
                now
        );
        TMS_CARRIER_STORAGE.put(carrier.carrierId(), carrier);
        auditLogService.recordForTenant(tenantId, "CREATE_TMS_CARRIER", "tms_carrier", carrier.carrierId());
        return carrier;
    }

    public TmsShipment createShipment(String tenantId,
                                      String operatorId,
                                      CreateTmsShipmentCommand command) {
        FulfillmentTask task = getTask(tenantId, command.fulfillmentTaskId());
        TmsCarrier carrier = requireOwnedCarrier(tenantId, command.carrierId());
        if (!task.storeId().equals(carrier.storeId())) {
            throw new BusinessException("7601", "鎵胯繍鍟嗕笉灞炰簬褰撳墠灞ョ害搴楅摵", HttpStatus.BAD_REQUEST);
        }
        requireOrder(task.orderId());
        OffsetDateTime now = OffsetDateTime.now();
        String sequenceValue = String.valueOf(TMS_SHIPMENT_SEQUENCE.incrementAndGet());
        TmsShipment shipment = new TmsShipment(
                "tms-shipment-" + sequenceValue,
                task.fulfillmentTaskId(),
                task.orderId(),
                task.storeId(),
                carrier.carrierId(),
                command.shippingMode(),
                command.freightAmount(),
                command.originCity(),
                command.destinationCity(),
                "in_transit",
                "generated",
                "pending",
                "pending",
                "created",
                0,
                operatorId,
                now
        );
        TMS_SHIPMENT_STORAGE.put(shipment.shipmentId(), shipment);
        auditLogService.recordForTenant(tenantId, "CREATE_TMS_SHIPMENT", "tms_shipment", shipment.shipmentId());
        return shipment;
    }

    public TmsTrackingEvent createTrackingEvent(String tenantId,
                                                String operatorId,
                                                String shipmentId,
                                                CreateTmsTrackingEventCommand command) {
        TmsShipment shipment = requireOwnedShipment(tenantId, shipmentId);
        OffsetDateTime now = OffsetDateTime.now();
        String sequenceValue = String.valueOf(TMS_TRACKING_EVENT_SEQUENCE.incrementAndGet());
        TmsTrackingEvent event = new TmsTrackingEvent(
                "tms-tracking-event-" + sequenceValue,
                shipment.shipmentId(),
                shipment.storeId(),
                command.trackingStatus(),
                command.locationText(),
                command.remark(),
                shipment.eventCount() + 1,
                operatorId,
                now
        );
        TMS_TRACKING_EVENT_STORAGE.put(event.trackingEventId(), event);
        TMS_SHIPMENT_STORAGE.put(shipment.shipmentId(), shipment.withTracking(command.trackingStatus()));
        auditLogService.recordForTenant(tenantId, "CREATE_TMS_TRACKING_EVENT", "tms_tracking_event", event.trackingEventId());
        return event;
    }

    public FreightSettlementRecord createFreightSettlement(String tenantId,
                                                           String operatorId,
                                                           CreateFreightSettlementCommand command) {
        TmsShipment shipment = requireOwnedShipment(tenantId, command.shipmentId());
        OffsetDateTime now = OffsetDateTime.now();
        String sequenceValue = String.valueOf(TMS_FREIGHT_SETTLEMENT_SEQUENCE.incrementAndGet());
        FreightSettlementRecord settlementRecord = new FreightSettlementRecord(
                "tms-freight-settlement-" + sequenceValue,
                shipment.shipmentId(),
                shipment.storeId(),
                command.settleMode(),
                command.costType(),
                command.billableWeight(),
                command.freightAmount(),
                "pending",
                operatorId,
                now
        );
        TMS_FREIGHT_SETTLEMENT_STORAGE.put(settlementRecord.settlementId(), settlementRecord);
        auditLogService.recordForTenant(tenantId, "CREATE_TMS_FREIGHT_SETTLEMENT", "tms_freight_settlement", settlementRecord.settlementId());
        return settlementRecord;
    }

    public TmsShipment signOffShipment(String tenantId,
                                       String operatorId,
                                       String shipmentId,
                                       SignOffShipmentCommand command) {
        TmsShipment shipment = requireOwnedShipment(tenantId, shipmentId);
        TmsShipment updatedShipment = shipment.withSignOff(command.signStatus());
        TMS_SHIPMENT_STORAGE.put(updatedShipment.shipmentId(), updatedShipment);
        auditLogService.recordForTenant(tenantId, "SIGN_OFF_TMS_SHIPMENT", "tms_shipment", shipmentId);
        return updatedShipment;
    }

    public ReverseLogisticsRecord createReverseLogistics(String tenantId,
                                                         String operatorId,
                                                         CreateReverseLogisticsCommand command) {
        OrderMain order = requireOrder(command.orderId());
        requireOwnedStore(tenantId, order.storeId());
        TmsShipment shipment = requireOwnedShipment(tenantId, command.shipmentId());
        TmsCarrier carrier = requireOwnedCarrier(tenantId, command.carrierId());
        if (!order.storeId().equals(shipment.storeId()) || !order.storeId().equals(carrier.storeId())) {
            throw new BusinessException("7602", "order shipment carrier store mismatch", HttpStatus.BAD_REQUEST);
        }
        OffsetDateTime now = OffsetDateTime.now();
        String sequenceValue = String.valueOf(TMS_REVERSE_LOGISTICS_SEQUENCE.incrementAndGet());
        ReverseLogisticsRecord reverseLogisticsRecord = new ReverseLogisticsRecord(
                "tms-reverse-logistics-" + sequenceValue,
                command.orderId(),
                command.shipmentId(),
                command.carrierId(),
                order.storeId(),
                command.reverseType(),
                "initiated",
                command.remark(),
                operatorId,
                now
        );
        TMS_REVERSE_LOGISTICS_STORAGE.put(reverseLogisticsRecord.reverseLogisticsId(), reverseLogisticsRecord);
        auditLogService.recordForTenant(tenantId, "CREATE_TMS_REVERSE_LOGISTICS", "tms_reverse_logistics", reverseLogisticsRecord.reverseLogisticsId());
        return reverseLogisticsRecord;
    }

    public ControlTowerView getControlTower(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        String storeId = TMS_SHIPMENT_STORAGE.values().stream()
                .filter(shipment -> storeIds.contains(shipment.storeId()))
                .map(TmsShipment::storeId)
                .findFirst()
                .orElseGet(() -> storeIds.isEmpty() ? null : storeIds.get(0));
        int carrierCount = (int) TMS_CARRIER_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .count();
        int shipmentCount = (int) TMS_SHIPMENT_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .count();
        int signedShipmentCount = (int) TMS_SHIPMENT_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .filter(record -> "delivered".equals(record.signStatus()))
                .count();
        int reverseLogisticsCount = (int) TMS_REVERSE_LOGISTICS_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .count();
        String latestTrackingStatus = TMS_TRACKING_EVENT_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .map(TmsTrackingEvent::latestTrackingStatus)
                .findFirst()
                .orElse("pending");
        int settlementPendingCount = (int) TMS_FREIGHT_SETTLEMENT_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .filter(record -> "pending".equals(record.settlementStatus()))
                .count();
        int podArchiveCount = (int) TMS_SHIPMENT_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .filter(record -> "archived".equals(record.podStatus()))
                .count();
        return new ControlTowerView(
                storeId,
                carrierCount,
                shipmentCount,
                signedShipmentCount,
                reverseLogisticsCount,
                latestTrackingStatus,
                settlementPendingCount,
                podArchiveCount
        );
    }

    public ExternalRoutePlanView planExternalRoute(String tenantId,
                                                   CreateExternalRoutePlanCommand command) {
        ExternalRoutingAdapter.RoutePlanResult result = externalRoutingAdapter.planRoute(
                new ExternalRoutingAdapter.RoutePlanRequest(
                        new ExternalRoutingAdapter.RoutingCoordinate(command.originLongitude(), command.originLatitude()),
                        new ExternalRoutingAdapter.RoutingCoordinate(command.destinationLongitude(), command.destinationLatitude()),
                        command.profile()
                )
        );
        auditLogService.recordForTenant(tenantId, "PLAN_EXTERNAL_ROUTE", "tms_external_route", result.provider());
        return new ExternalRoutePlanView(
                result.provider(),
                result.profile(),
                result.routeStatus(),
                result.usedFallback(),
                result.fallbackReason(),
                result.waypointCount(),
                result.distanceMeters(),
                result.durationSeconds(),
                result.configured(),
                result.host(),
                result.maskedEndpoint()
        );
    }

    public ExternalDistanceMatrixView calculateExternalDistanceMatrix(String tenantId,
                                                                     CreateExternalDistanceMatrixCommand command) {
        if (command.coordinates() == null || command.coordinates().size() < 2) {
            throw new BusinessException("1004", "at least two coordinates are required", HttpStatus.BAD_REQUEST);
        }
        ExternalRoutingAdapter.DistanceMatrixResult result = externalRoutingAdapter.calculateDistanceMatrix(
                new ExternalRoutingAdapter.DistanceMatrixRequest(
                        command.coordinates().stream()
                                .map(coordinate -> new ExternalRoutingAdapter.RoutingCoordinate(
                                        coordinate.longitude(),
                                        coordinate.latitude()
                                ))
                                .toList(),
                        command.profile()
                )
        );
        auditLogService.recordForTenant(tenantId, "CALCULATE_EXTERNAL_DISTANCE_MATRIX", "tms_external_route", result.provider());
        return new ExternalDistanceMatrixView(
                result.provider(),
                result.profile(),
                result.matrixStatus(),
                result.usedFallback(),
                result.fallbackReason(),
                result.coordinateCount(),
                result.distanceMatrixMeters(),
                result.durationMatrixSeconds(),
                result.configured(),
                result.host(),
                result.maskedEndpoint()
        );
    }

    public void clear() {
        TMS_CARRIER_STORAGE.clear();
        TMS_CARRIER_SEQUENCE.set(0);
        TMS_SHIPMENT_STORAGE.clear();
        TMS_SHIPMENT_SEQUENCE.set(0);
        TMS_TRACKING_EVENT_STORAGE.clear();
        TMS_TRACKING_EVENT_SEQUENCE.set(0);
        TMS_FREIGHT_SETTLEMENT_STORAGE.clear();
        TMS_FREIGHT_SETTLEMENT_SEQUENCE.set(0);
        TMS_REVERSE_LOGISTICS_STORAGE.clear();
        TMS_REVERSE_LOGISTICS_SEQUENCE.set(0);
        logisticsRecordRepository.deleteAll();
        fulfillmentTaskRepository.deleteAll();
    }

    private TmsCarrier requireOwnedCarrier(String tenantId, String carrierId) {
        TmsCarrier carrier = TMS_CARRIER_STORAGE.get(carrierId);
        if (carrier == null) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        requireOwnedStore(tenantId, carrier.storeId());
        return carrier;
    }

    private TmsShipment requireOwnedShipment(String tenantId, String shipmentId) {
        TmsShipment shipment = TMS_SHIPMENT_STORAGE.get(shipmentId);
        if (shipment == null) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        requireOwnedStore(tenantId, shipment.storeId());
        return shipment;
    }

    private OrderMain requireOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    public record FulfillmentReplayView(
            String fulfillmentTaskId,
            String status,
            int replayCount
    ) {
    }

    public record CreateTmsCarrierCommand(
            String storeId,
            String carrierName,
            String carrierCode,
            String channelType,
            String serviceScope
    ) {
    }

    public record TmsCarrier(
            String carrierId,
            String storeId,
            String carrierName,
            String carrierCode,
            String channelType,
            String serviceScope,
            String carrierStatus,
            String operatorId,
            OffsetDateTime createdAt
    ) {
    }

    public record CreateTmsShipmentCommand(
            String fulfillmentTaskId,
            String carrierId,
            String shippingMode,
            BigDecimal freightAmount,
            String originCity,
            String destinationCity
    ) {
    }

    public record TmsShipment(
            String shipmentId,
            String fulfillmentTaskId,
            String orderId,
            String storeId,
            String carrierId,
            String shippingMode,
            BigDecimal freightAmount,
            String originCity,
            String destinationCity,
            String shipmentStatus,
            String labelStatus,
            String signStatus,
            String podStatus,
            String latestTrackingStatus,
            int eventCount,
            String operatorId,
            OffsetDateTime createdAt
    ) {
        public TmsShipment withTracking(String latestTrackingStatus) {
            return new TmsShipment(
                    shipmentId,
                    fulfillmentTaskId,
                    orderId,
                    storeId,
                    carrierId,
                    shippingMode,
                    freightAmount,
                    originCity,
                    destinationCity,
                    shipmentStatus,
                    labelStatus,
                    signStatus,
                    podStatus,
                    latestTrackingStatus,
                    eventCount + 1,
                    operatorId,
                    createdAt
            );
        }

        public TmsShipment withSignOff(String signStatus) {
            return new TmsShipment(
                    shipmentId,
                    fulfillmentTaskId,
                    orderId,
                    storeId,
                    carrierId,
                    shippingMode,
                    freightAmount,
                    originCity,
                    destinationCity,
                    shipmentStatus,
                    labelStatus,
                    signStatus,
                    "archived",
                    latestTrackingStatus,
                    eventCount,
                    operatorId,
                    createdAt
            );
        }
    }

    public record CreateTmsTrackingEventCommand(
            String trackingStatus,
            String locationText,
            String remark
    ) {
    }

    public record TmsTrackingEvent(
            String trackingEventId,
            String shipmentId,
            String storeId,
            String latestTrackingStatus,
            String locationText,
            String remark,
            int eventCount,
            String operatorId,
            OffsetDateTime createdAt
    ) {
    }

    public record CreateFreightSettlementCommand(
            String shipmentId,
            String settleMode,
            String costType,
            BigDecimal billableWeight,
            BigDecimal freightAmount
    ) {
    }

    public record FreightSettlementRecord(
            String settlementId,
            String shipmentId,
            String storeId,
            String settleMode,
            String costType,
            BigDecimal billableWeight,
            BigDecimal freightAmount,
            String settlementStatus,
            String operatorId,
            OffsetDateTime createdAt
    ) {
    }

    public record SignOffShipmentCommand(
            String signStatus,
            String proofType,
            String remark
    ) {
    }

    public record CreateReverseLogisticsCommand(
            String orderId,
            String shipmentId,
            String carrierId,
            String reverseType,
            String remark
    ) {
    }

    public record ReverseLogisticsRecord(
            String reverseLogisticsId,
            String orderId,
            String shipmentId,
            String carrierId,
            String storeId,
            String reverseType,
            String reverseStatus,
            String remark,
            String operatorId,
            OffsetDateTime createdAt
    ) {
    }

    public record ControlTowerView(
            String storeId,
            int carrierCount,
            int shipmentCount,
            int signedShipmentCount,
            int reverseLogisticsCount,
            String latestTrackingStatus,
            int settlementPendingCount,
            int podArchiveCount
    ) {
    }

    public record CreateExternalRoutePlanCommand(
            double originLongitude,
            double originLatitude,
            double destinationLongitude,
            double destinationLatitude,
            String profile
    ) {
    }

    public record ExternalRoutePlanView(
            String provider,
            String profile,
            String routeStatus,
            boolean usedFallback,
            String fallbackReason,
            int waypointCount,
            int distanceMeters,
            int durationSeconds,
            boolean configured,
            String host,
            String maskedEndpoint
    ) {
    }

    public record RoutingCoordinateInput(
            double longitude,
            double latitude
    ) {
    }

    public record CreateExternalDistanceMatrixCommand(
            List<RoutingCoordinateInput> coordinates,
            String profile
    ) {
    }

    public record ExternalDistanceMatrixView(
            String provider,
            String profile,
            String matrixStatus,
            boolean usedFallback,
            String fallbackReason,
            int coordinateCount,
            List<List<Integer>> distanceMatrixMeters,
            List<List<Integer>> durationMatrixSeconds,
            boolean configured,
            String host,
            String maskedEndpoint
    ) {
    }
}

