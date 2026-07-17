package backend.order.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.fulfillment.domain.repository.FulfillmentTaskRepository;
import backend.fulfillment.model.FulfillmentTask;
import backend.order.domain.repository.OrderItemRepository;
import backend.order.domain.repository.OrderRepository;
import backend.order.dto.CreateOrderSyncRequest;
import backend.order.dto.OrderDetailView;
import backend.order.dto.OrderSyncResult;
import backend.order.model.OrderItem;
import backend.order.model.OrderMain;
import backend.product.domain.repository.ProductRepository;
import backend.product.model.Product;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderApplicationService {

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final Map<String, OrderAuditReviewView> orderAuditReviews = new LinkedHashMap<>();
    private final Map<String, OrderSplitView> orderSplits = new LinkedHashMap<>();
    private final Map<String, OrderMergeView> orderMergeGroups = new LinkedHashMap<>();
    private final Map<String, String> orderMergeGroupIndex = new LinkedHashMap<>();
    private final Map<String, OrderRoutePlanView> orderRoutePlans = new LinkedHashMap<>();
    private final Map<String, OrderReverseStatusView> orderReverseStatuses = new LinkedHashMap<>();
    private int mergeSequence = 1;

    public OrderApplicationService(AuditLogService auditLogService,
                                   StoreRepository storeRepository,
                                   ProductRepository productRepository,
                                   OrderRepository orderRepository,
                                   OrderItemRepository orderItemRepository,
                                   FulfillmentTaskRepository fulfillmentTaskRepository) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.fulfillmentTaskRepository = fulfillmentTaskRepository;
    }

    public List<OrderMain> listOrders(String tenantId) {
        return orderRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public List<StandardizedOrderView> listStandardizedOrders(String tenantId) {
        List<OrderMain> orders = listOrders(tenantId);
        return orders.stream()
                .map(order -> toStandardizedOrderView(tenantId, order))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    public OrderDetailView getOrder(String tenantId, String orderId) {
        OrderMain order = requireOwnedOrder(tenantId, orderId);
        return new OrderDetailView(order, orderItemRepository.findByOrderId(orderId));
    }

    public OrderSyncResult syncOrder(String tenantId, CreateOrderSyncRequest request) {
        Store store = requireOwnedStore(tenantId, request.storeId());
        Product product = resolveProduct(tenantId, store.storeId(), request.productId());
        String platformOrderId = fallback(request.platformOrderId(), "sync-" + UUID.randomUUID().toString().substring(0, 8));
        String skuId = fallback(request.skuId(), "sku-9001");
        int quantity = request.quantity() == null ? 1 : request.quantity();
        BigDecimal unitPrice = request.unitPrice() == null ? BigDecimal.valueOf(99.90) : request.unitPrice();

        OrderMain existing = orderRepository.findByStoreAndPlatformOrderId(store.storeId(), platformOrderId).orElse(null);
        if (existing != null) {
            FulfillmentTask existingTask = ensureFulfillmentTask(existing);
            return new OrderSyncResult(
                    buildSyncTaskId(existing.orderId()),
                    existing.storeId(),
                    existing.orderId(),
                    existingTask.fulfillmentTaskId(),
                    true
            );
        }

        OrderMain order = orderRepository.save(new OrderMain(
                null,
                store.storeId(),
                platformOrderId,
                "pending_fulfillment",
                "pending",
                unitPrice.multiply(BigDecimal.valueOf(quantity)),
                BigDecimal.valueOf(20),
                fallback(request.buyerName(), "绯荤粺鍚屾涔板"),
                fallback(request.buyerPhoneMask(), "138****0000"),
                fallback(request.shippingAddress(), "榛樿鍚屾鍦板潃"),
                OffsetDateTime.now().plusHours(24),
                OffsetDateTime.now()
        ));
        orderItemRepository.save(new OrderItem(
                null,
                order.orderId(),
                product.productId(),
                skuId,
                quantity,
                unitPrice,
                OffsetDateTime.now()
        ));
        FulfillmentTask fulfillmentTask = createFulfillmentTask(order);
        auditLogService.recordForTenant(tenantId, "SYNC_ORDER", "order_main", order.orderId());
        return new OrderSyncResult(
                buildSyncTaskId(order.orderId()),
                order.storeId(),
                order.orderId(),
                fulfillmentTask.fulfillmentTaskId(),
                false
        );
    }

    public OrderAuditReviewView auditOrder(String tenantId, String orderId) {
        OrderMain order = requireOwnedOrder(tenantId, orderId);
        List<String> riskTags = new ArrayList<>();
        if (order.totalAmount().compareTo(BigDecimal.valueOf(5000)) >= 0) {
            riskTags.add("high_amount");
        }
        boolean addressValid = order.shippingAddress() != null && order.shippingAddress().trim().length() >= 8;
        if (!addressValid) {
            riskTags.add("address_invalid");
        }
        if (order.buyerPhoneMask() == null || order.buyerPhoneMask().isBlank()) {
            riskTags.add("buyer_phone_missing");
        }
        String riskLevel = riskTags.isEmpty()
                ? "low"
                : riskTags.contains("high_amount") || riskTags.size() >= 2
                ? "high"
                : "medium";
        OrderAuditReviewView review = new OrderAuditReviewView(
                order.orderId(),
                order.storeId(),
                riskLevel,
                addressValid,
                riskTags,
                OffsetDateTime.now()
        );
        orderAuditReviews.put(orderId, review);
        auditLogService.recordForTenant(tenantId, "AUDIT_ORDER", "order_main", orderId);
        return review;
    }

    public OrderSplitView splitOrder(String tenantId, String orderId, SaveOrderSplitRequest request) {
        OrderMain order = requireOwnedOrder(tenantId, orderId);
        OrderItem primaryItem = requirePrimaryItem(order.orderId());
        List<OrderSplitChildView> children = request.children() == null ? List.of() : request.children();
        if (children.isEmpty()) {
            throw new BusinessException("7701", "split children are required", HttpStatus.BAD_REQUEST);
        }
        int totalQuantity = children.stream()
                .map(OrderSplitChildView::quantity)
                .reduce(0, Integer::sum);
        if (totalQuantity != primaryItem.quantity()) {
            throw new BusinessException("7702", "split quantity must equal original order item quantity", HttpStatus.BAD_REQUEST);
        }
        List<OrderSplitChildView> normalizedChildren = new ArrayList<>();
        for (int index = 0; index < children.size(); index++) {
            OrderSplitChildView child = children.get(index);
            normalizedChildren.add(new OrderSplitChildView(
                    order.orderId() + "-split-" + (index + 1),
                    child.childLabel(),
                    child.quantity(),
                    normalizeOrderType(child.orderType())
            ));
        }
        OrderSplitView splitView = new OrderSplitView(
                order.orderId(),
                order.storeId(),
                "split_completed",
                request.remark(),
                normalizedChildren,
                OffsetDateTime.now()
        );
        orderSplits.put(orderId, splitView);
        auditLogService.recordForTenant(tenantId, "SPLIT_ORDER", "order_main", orderId);
        return splitView;
    }

    public OrderMergeView mergeOrders(String tenantId, SaveOrderMergeRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        if (request.orderIds() == null || request.orderIds().size() < 2) {
            throw new BusinessException("7703", "at least two orders are required for merge", HttpStatus.BAD_REQUEST);
        }
        for (String orderId : request.orderIds()) {
            OrderMain order = requireOwnedOrder(tenantId, orderId);
            if (!request.storeId().equals(order.storeId())) {
                throw new BusinessException("7704", "all merge orders must belong to the same store", HttpStatus.BAD_REQUEST);
            }
        }
        OrderMergeView mergeView = new OrderMergeView(
                "order-merge-" + mergeSequence++,
                request.storeId(),
                request.orderIds().stream().distinct().toList(),
                normalizeOrderType(request.mergedOrderType()),
                request.remark(),
                OffsetDateTime.now()
        );
        orderMergeGroups.put(mergeView.mergeGroupId(), mergeView);
        mergeView.orderIds().forEach(orderId -> orderMergeGroupIndex.put(orderId, mergeView.mergeGroupId()));
        auditLogService.recordForTenant(tenantId, "MERGE_ORDER_GROUP", "order_merge_group", mergeView.mergeGroupId());
        return mergeView;
    }

    public OrderRoutePlanView planRoute(String tenantId, String orderId, SaveOrderRoutePlanRequest request) {
        OrderMain order = requireOwnedOrder(tenantId, orderId);
        List<String> warehouseCandidates = request.warehouseCandidates() == null || request.warehouseCandidates().isEmpty()
                ? List.of("WH-DEFAULT")
                : request.warehouseCandidates();
        OrderRoutePlanView routePlan = new OrderRoutePlanView(
                order.orderId(),
                order.storeId(),
                derivePriorityCode(order),
                warehouseCandidates.get(0),
                fallback(request.shippingStrategy(), "standard_delivery"),
                OffsetDateTime.now()
        );
        orderRoutePlans.put(orderId, routePlan);
        auditLogService.recordForTenant(tenantId, "PLAN_ORDER_ROUTE", "order_main", orderId);
        return routePlan;
    }

    public OrderReverseStatusView updateReverseStatus(String tenantId, String orderId, SaveOrderReverseStatusRequest request) {
        OrderMain order = requireOwnedOrder(tenantId, orderId);
        String reverseStatus = normalizeReverseStatus(request.reverseStatus());
        OrderReverseStatusView view = new OrderReverseStatusView(
                order.orderId(),
                order.storeId(),
                reverseStatus,
                deriveReverseCategory(reverseStatus),
                request.remark(),
                OffsetDateTime.now()
        );
        orderReverseStatuses.put(orderId, view);
        auditLogService.recordForTenant(tenantId, "UPDATE_ORDER_REVERSE_STATUS", "order_main", orderId);
        return view;
    }

    public OrderOrchestrationView getOrchestrationView(String tenantId, String orderId) {
        OrderMain order = requireOwnedOrder(tenantId, orderId);
        FulfillmentTask fulfillmentTask = ensureFulfillmentTask(order);
        OrderSplitView splitView = orderSplits.get(orderId);
        OrderMergeView mergeView = orderMergeGroups.get(orderMergeGroupIndex.get(orderId));
        OrderRoutePlanView routePlanView = orderRoutePlans.get(orderId);
        return new OrderOrchestrationView(
                order.orderId(),
                order.storeId(),
                fulfillmentTask.fulfillmentTaskId(),
                splitView == null ? 0 : splitView.children().size(),
                mergeView == null ? 0 : mergeView.orderIds().size(),
                routePlanView == null ? null : routePlanView.routeWarehouseCode(),
                "ready",
                "pending",
                "pending"
        );
    }

    public OmsWorkbenchView getOmsWorkbench(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return new OmsWorkbenchView(
                listOrders(tenantId).size(),
                (int) orderAuditReviews.values().stream()
                        .filter(item -> storeIds.contains(item.storeId()))
                        .filter(item -> !"low".equals(item.riskLevel()))
                        .count(),
                (int) orderSplits.values().stream()
                        .filter(item -> storeIds.contains(item.storeId()))
                        .count(),
                (int) orderMergeGroups.values().stream()
                        .filter(item -> storeIds.contains(item.storeId()))
                        .count(),
                (int) fulfillmentTaskRepository.findByStoreIds(storeIds).stream()
                        .filter(task -> task.retryCount() != null && task.retryCount() > 0)
                        .count(),
                (int) orderReverseStatuses.values().stream()
                        .filter(item -> storeIds.contains(item.storeId()))
                        .count()
        );
    }

    public void clear() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        orderAuditReviews.clear();
        orderSplits.clear();
        orderMergeGroups.clear();
        orderMergeGroupIndex.clear();
        orderRoutePlans.clear();
        orderReverseStatuses.clear();
        mergeSequence = 1;
    }

    private StandardizedOrderView toStandardizedOrderView(String tenantId, OrderMain order) {
        Store store = requireOwnedStore(tenantId, order.storeId());
        return new StandardizedOrderView(
                order.orderId(),
                order.storeId(),
                order.platformOrderId(),
                store.platformType(),
                order.orderStatus(),
                order.totalAmount(),
                order.createdAt()
        );
    }

    private FulfillmentTask ensureFulfillmentTask(OrderMain order) {
        return fulfillmentTaskRepository.findByOrderId(order.orderId()).stream()
                .findFirst()
                .orElseGet(() -> createFulfillmentTask(order));
    }

    private FulfillmentTask createFulfillmentTask(OrderMain order) {
        return fulfillmentTaskRepository.save(new FulfillmentTask(
                null,
                order.storeId(),
                order.orderId(),
                "fulfill-" + order.platformOrderId(),
                "pending_confirm",
                0,
                OffsetDateTime.now().plusHours(4),
                null,
                OffsetDateTime.now()
        ));
    }

    private OrderMain requireOwnedOrder(String tenantId, String orderId) {
        OrderMain order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, order.storeId());
        return order;
    }

    private OrderItem requirePrimaryItem(String orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
    }

    private Product resolveProduct(String tenantId, String storeId, String productId) {
        if (productId != null && !productId.isBlank()) {
            Product product = requireOwnedProduct(tenantId, productId);
            requireSameStore(storeId, product.storeId(), "product does not belong to the current store");
            return product;
        }
        return productRepository.findByStoreIds(List.of(storeId)).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("7502", "current store has no available product", HttpStatus.BAD_REQUEST));
    }

    private Product requireOwnedProduct(String tenantId, String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, product.storeId());
        return product;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "tenant context forbidden", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private void requireSameStore(String expectedStoreId, String actualStoreId, String message) {
        if (!expectedStoreId.equals(actualStoreId)) {
            throw new BusinessException("7501", message, HttpStatus.BAD_REQUEST);
        }
    }

    private String derivePriorityCode(OrderMain order) {
        if (order.totalAmount().compareTo(BigDecimal.valueOf(200)) >= 0) {
            return "high";
        }
        return "normal";
    }

    private String normalizeOrderType(String orderType) {
        String normalized = orderType == null || orderType.isBlank()
                ? "normal"
                : orderType.trim().toLowerCase(Locale.ROOT);
        if (!List.of("normal", "gift", "combined").contains(normalized)) {
            throw new BusinessException("7705", "unsupported orderType", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeReverseStatus(String reverseStatus) {
        String normalized = reverseStatus == null ? "" : reverseStatus.trim().toLowerCase(Locale.ROOT);
        if (!List.of("refunding", "refunded", "after_sale", "reverse_completed").contains(normalized)) {
            throw new BusinessException("7706", "unsupported reverseStatus", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String deriveReverseCategory(String reverseStatus) {
        return List.of("refunding", "refunded").contains(reverseStatus) ? "refund" : "reverse";
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String buildSyncTaskId(String orderId) {
        return "order-sync-" + orderId;
    }

    public record StandardizedOrderView(
            String orderId,
            String storeId,
            String platformOrderId,
            String sourcePlatform,
            String standardOrderStatus,
            BigDecimal totalAmount,
            OffsetDateTime createdAt
    ) {
    }

    public record OrderAuditReviewView(
            String orderId,
            String storeId,
            String riskLevel,
            boolean addressValid,
            List<String> riskTags,
            OffsetDateTime reviewedAt
    ) {
    }

    public record SaveOrderSplitRequest(
            String remark,
            List<OrderSplitChildView> children
    ) {
    }

    public record OrderSplitChildView(
            String childOrderId,
            String childLabel,
            Integer quantity,
            String orderType
    ) {
    }

    public record OrderSplitView(
            String parentOrderId,
            String storeId,
            String splitStatus,
            String remark,
            List<OrderSplitChildView> children,
            OffsetDateTime updatedAt
    ) {
    }

    public record SaveOrderMergeRequest(
            String storeId,
            List<String> orderIds,
            String mergedOrderType,
            String remark
    ) {
    }

    public record OrderMergeView(
            String mergeGroupId,
            String storeId,
            List<String> orderIds,
            String mergedOrderType,
            String remark,
            OffsetDateTime createdAt
    ) {
    }

    public record SaveOrderRoutePlanRequest(
            List<String> warehouseCandidates,
            String shippingStrategy
    ) {
    }

    public record OrderRoutePlanView(
            String orderId,
            String storeId,
            String priorityCode,
            String routeWarehouseCode,
            String shippingStrategy,
            OffsetDateTime plannedAt
    ) {
    }

    public record SaveOrderReverseStatusRequest(
            String reverseStatus,
            String remark
    ) {
    }

    public record OrderReverseStatusView(
            String orderId,
            String storeId,
            String reverseStatus,
            String reverseCategory,
            String remark,
            OffsetDateTime updatedAt
    ) {
    }

    public record OrderOrchestrationView(
            String orderId,
            String storeId,
            String fulfillmentTaskId,
            int splitChildCount,
            int mergeOrderCount,
            String routeWarehouseCode,
            String wmsSyncStatus,
            String tmsSyncStatus,
            String financeSyncStatus
    ) {
    }

    public record OmsWorkbenchView(
            int standardizedOrderCount,
            int riskOrderCount,
            int splitOrderCount,
            int mergedOrderGroupCount,
            int manualReplayCount,
            int reverseOrderCount
    ) {
    }
}

