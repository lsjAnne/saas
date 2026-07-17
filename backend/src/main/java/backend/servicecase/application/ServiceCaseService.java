package backend.servicecase.application;

import backend.approval.application.ApprovalService;
import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderMain;
import backend.servicecase.domain.repository.AfterSaleRecordRepository;
import backend.servicecase.domain.repository.CustomerServiceTicketRepository;
import backend.servicecase.dto.CreateAfterSaleRequest;
import backend.servicecase.model.AfterSaleRecord;
import backend.servicecase.model.CustomerServiceTicket;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServiceCaseService {

    private static final long DEFAULT_TICKET_SLA_MINUTES = 30L;

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final CustomerServiceTicketRepository customerServiceTicketRepository;
    private final AfterSaleRecordRepository afterSaleRecordRepository;
    private final ApprovalService approvalService;
    private final Map<String, OffsetDateTime> firstResponseTimes = new LinkedHashMap<>();
    private final Map<String, TicketSatisfactionView> ticketSatisfactions = new LinkedHashMap<>();
    private final Map<String, AfterSaleRecord> afterSaleMirror = new LinkedHashMap<>();

    public ServiceCaseService(AuditLogService auditLogService,
                              StoreRepository storeRepository,
                              OrderRepository orderRepository,
                              CustomerServiceTicketRepository customerServiceTicketRepository,
                              AfterSaleRecordRepository afterSaleRecordRepository,
                              ApprovalService approvalService) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.orderRepository = orderRepository;
        this.customerServiceTicketRepository = customerServiceTicketRepository;
        this.afterSaleRecordRepository = afterSaleRecordRepository;
        this.approvalService = approvalService;
    }

    public List<CustomerServiceTicket> listTickets(String tenantId) {
        return customerServiceTicketRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public List<CustomerServiceTicket> listTicketsByStore(String tenantId, String storeId) {
        requireOwnedStore(tenantId, storeId);
        return customerServiceTicketRepository.findByStoreIds(List.of(storeId));
    }

    public List<CustomerServiceTicket> listTicketsByCustomer(String tenantId, String storeId, String customerId) {
        return listTicketsByStore(tenantId, storeId).stream()
                .filter(ticket -> customerId.equals(ticket.customerId()))
                .toList();
    }

    public CustomerServiceTicket getTicket(String tenantId, String ticketId) {
        CustomerServiceTicket ticket = customerServiceTicketRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, ticket.storeId());
        return ticket;
    }

    public CustomerServiceTicket generateReplySuggestion(String tenantId, String ticketId) {
        CustomerServiceTicket ticket = getTicket(tenantId, ticketId);
        OrderMain order = requireOwnedOrder(tenantId, ticket.orderId());
        String suggestion = """
                鎮ㄥソ锛屽叧浜庤鍗?%s锛岀郴缁熷綋鍓嶇姸鎬佷负 %s銆傚缓璁厛鍚戝鎴峰悓姝ュ鐞嗚繘灞曪紝濡傛秹鍙婇€€娆炬垨琛ュ彂锛屽皢鍦ㄦ牳瀹炲悗绗竴鏃堕棿澶勭悊銆?                """.formatted(order.platformOrderId(), order.orderStatus()).trim();
        CustomerServiceTicket updated = customerServiceTicketRepository.save(ticket.withSuggestion("processing", suggestion));
        firstResponseTimes.putIfAbsent(ticketId, OffsetDateTime.now());
        auditLogService.recordForTenant(tenantId, "GENERATE_TICKET_REPLY_SUGGESTION", "customer_service_ticket", ticketId);
        return updated;
    }

    public TicketSatisfactionView saveTicketSatisfaction(String tenantId,
                                                         String ticketId,
                                                         SaveTicketSatisfactionRequest request) {
        CustomerServiceTicket ticket = getTicket(tenantId, ticketId);
        if (request.score() == null || request.score() < 1 || request.score() > 5) {
            throw new BusinessException("7601", "ticket satisfaction score must be between 1 and 5", HttpStatus.BAD_REQUEST);
        }
        TicketSatisfactionView satisfaction = new TicketSatisfactionView(
                ticket.ticketId(),
                ticket.storeId(),
                request.score(),
                request.comment(),
                OffsetDateTime.now()
        );
        ticketSatisfactions.put(ticketId, satisfaction);
        auditLogService.recordForTenant(tenantId, "SAVE_TICKET_SATISFACTION", "customer_service_ticket", ticketId);
        return satisfaction;
    }

    public TicketSlaOverviewView getTicketSlaOverview(String tenantId, String storeId) {
        List<CustomerServiceTicket> tickets = storeId == null || storeId.isBlank()
                ? listTickets(tenantId)
                : listTicketsByStore(tenantId, storeId);
        long withinSlaCount = tickets.stream()
                .filter(ticket -> isWithinSla(ticket, firstResponseTimes.get(ticket.ticketId())))
                .count();
        long overdueTicketCount = tickets.stream()
                .filter(ticket -> isOverdue(ticket, firstResponseTimes.get(ticket.ticketId())))
                .count();
        long respondedCount = tickets.stream()
                .filter(ticket -> firstResponseTimes.containsKey(ticket.ticketId()))
                .count();
        BigDecimal totalResponseMinutes = tickets.stream()
                .filter(ticket -> firstResponseTimes.containsKey(ticket.ticketId()))
                .map(ticket -> BigDecimal.valueOf(ChronoUnit.MINUTES.between(ticket.createdAt(), firstResponseTimes.get(ticket.ticketId()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageResponseMinutes = respondedCount == 0
                ? BigDecimal.ZERO
                : totalResponseMinutes.divide(BigDecimal.valueOf(respondedCount), 2, RoundingMode.HALF_UP);
        List<TicketSatisfactionView> satisfactions = tickets.stream()
                .map(ticket -> ticketSatisfactions.get(ticket.ticketId()))
                .filter(item -> item != null)
                .toList();
        BigDecimal totalScore = satisfactions.stream()
                .map(item -> BigDecimal.valueOf(item.score()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageSatisfactionScore = satisfactions.isEmpty()
                ? BigDecimal.ZERO
                : totalScore.divide(BigDecimal.valueOf(satisfactions.size()), 2, RoundingMode.HALF_UP);
        return new TicketSlaOverviewView(
                tickets.size(),
                (int) withinSlaCount,
                (int) overdueTicketCount,
                averageResponseMinutes,
                satisfactions.size(),
                averageSatisfactionScore
        );
    }

    public AfterSaleRecord createAfterSale(String tenantId, CreateAfterSaleRequest request) {
        OrderMain order = requireOwnedOrder(tenantId, request.orderId());
        AfterSaleRecord afterSaleRecord = afterSaleRecordRepository.save(new AfterSaleRecord(
                null,
                order.orderId(),
                request.afterSaleType(),
                request.reasonText(),
                "created",
                request.evidenceBlob(),
                OffsetDateTime.now()
        ));
        afterSaleMirror.put(afterSaleRecord.afterSaleId(), afterSaleRecord);
        auditLogService.recordForTenant(tenantId, "CREATE_AFTER_SALE", "after_sale_record", afterSaleRecord.afterSaleId());
        return afterSaleRecord;
    }

    public AfterSaleRecord getAfterSale(String tenantId, String afterSaleId) {
        AfterSaleRecord afterSaleRecord = afterSaleRecordRepository.findByAfterSaleId(afterSaleId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedOrder(tenantId, afterSaleRecord.orderId());
        return afterSaleRecord;
    }

    public List<AfterSaleRecord> listAfterSalesByOrderIds(String tenantId, List<String> orderIds) {
        return afterSaleMirror.values().stream()
                .filter(record -> orderIds.contains(record.orderId()))
                .filter(record -> {
                    requireOwnedOrder(tenantId, record.orderId());
                    return true;
                })
                .toList();
    }

    public AfterSaleRecord submitApproval(String tenantId, String operatorId, String afterSaleId) {
        AfterSaleRecord current = getAfterSale(tenantId, afterSaleId);
        if (!"created".equals(current.status())) {
            throw new BusinessException("1004", "current status does not allow submit approval", HttpStatus.BAD_REQUEST);
        }
        approvalService.createApprovalIfAbsent(
                tenantId,
                operatorId,
                "after_sale_submit",
                "after_sale_record",
                afterSaleId,
                operatorId,
                current.reasonText()
        );
        AfterSaleRecord updated = afterSaleRecordRepository.save(current.withStatus("reviewing"));
        afterSaleMirror.put(afterSaleId, updated);
        OrderMain order = requireOwnedOrder(tenantId, current.orderId());
        orderRepository.save(order.withStatus("after_sale", order.logisticsStatus()));
        auditLogService.recordForTenant(tenantId, "SUBMIT_AFTER_SALE_APPROVAL", "after_sale_record", afterSaleId);
        return updated;
    }

    public CustomerServiceTicket createTicketSeed(String tenantId,
                                                  String storeId,
                                                  String orderId,
                                                  String customerId,
                                                  boolean riskFlag) {
        requireOwnedStore(tenantId, storeId);
        requireOwnedOrder(tenantId, orderId);
        return customerServiceTicketRepository.save(new CustomerServiceTicket(
                null,
                storeId,
                orderId,
                customerId,
                "open",
                riskFlag,
                null,
                OffsetDateTime.now()
        ));
    }

    public void clear() {
        afterSaleRecordRepository.deleteAll();
        customerServiceTicketRepository.deleteAll();
        firstResponseTimes.clear();
        ticketSatisfactions.clear();
        afterSaleMirror.clear();
    }

    private boolean isWithinSla(CustomerServiceTicket ticket, OffsetDateTime firstResponseAt) {
        return firstResponseAt != null
                && ChronoUnit.MINUTES.between(ticket.createdAt(), firstResponseAt) <= DEFAULT_TICKET_SLA_MINUTES;
    }

    private boolean isOverdue(CustomerServiceTicket ticket, OffsetDateTime firstResponseAt) {
        if (firstResponseAt != null) {
            return ChronoUnit.MINUTES.between(ticket.createdAt(), firstResponseAt) > DEFAULT_TICKET_SLA_MINUTES;
        }
        return ChronoUnit.MINUTES.between(ticket.createdAt(), OffsetDateTime.now()) > DEFAULT_TICKET_SLA_MINUTES;
    }

    private OrderMain requireOwnedOrder(String tenantId, String orderId) {
        OrderMain order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, order.storeId());
        return order;
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

    public record SaveTicketSatisfactionRequest(
            Integer score,
            String comment
    ) {
    }

    public record TicketSatisfactionView(
            String ticketId,
            String storeId,
            int score,
            String comment,
            OffsetDateTime ratedAt
    ) {
    }

    public record TicketSlaOverviewView(
            int totalTicketCount,
            int withinSlaCount,
            int overdueTicketCount,
            BigDecimal averageFirstResponseMinutes,
            int satisfactionCount,
            BigDecimal averageSatisfactionScore
    ) {
    }
}

