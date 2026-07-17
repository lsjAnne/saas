package backend.supplier.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import backend.supplier.domain.repository.SupplierRepository;
import backend.supplier.dto.CreateSupplierRequest;
import backend.supplier.dto.UpdateSupplierRequest;
import backend.supplier.model.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SupplierService {

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final SupplierRepository supplierRepository;
    private final Map<String, SupplierAdmissionReviewView> admissionReviews = new LinkedHashMap<>();
    private final Map<String, SupplierScorecardView> supplierScorecards = new LinkedHashMap<>();
    private final Map<String, SupplierInquiryView> supplierInquiries = new LinkedHashMap<>();
    private final Map<String, List<String>> supplierInquiryIndex = new LinkedHashMap<>();
    private final Map<String, List<SupplierDeliveryAppointmentView>> deliveryAppointments = new LinkedHashMap<>();
    private final Map<String, List<SupplierSettlementStatementView>> settlementStatements = new LinkedHashMap<>();
    private final Map<String, List<SupplierRiskEventView>> riskEvents = new LinkedHashMap<>();
    private int inquirySequence = 1;
    private int appointmentSequence = 1;
    private int settlementSequence = 1;
    private int riskSequence = 1;

    public SupplierService(AuditLogService auditLogService,
                           StoreRepository storeRepository,
                           SupplierRepository supplierRepository) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.supplierRepository = supplierRepository;
    }

    public List<Supplier> listSuppliers(String tenantId) {
        return supplierRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public Supplier createSupplier(String tenantId, CreateSupplierRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        supplierRepository.findByStoreAndPlatformSupplier(
                request.storeId(),
                request.supplierPlatformType(),
                request.supplierPlatformId()
        ).ifPresent(existing -> {
            throw new BusinessException("7201", "supplier already exists for the same platform supplier id", HttpStatus.BAD_REQUEST);
        });
        Supplier supplier = supplierRepository.save(new Supplier(
                null,
                request.storeId(),
                request.supplierPlatformType(),
                request.supplierPlatformId(),
                request.supplierName(),
                request.sourceUrl(),
                request.priceScore(),
                request.deliveryScore(),
                request.stabilityScore(),
                request.riskLevel(),
                Boolean.TRUE.equals(request.dropshipSupportFlag()),
                false,
                false,
                false,
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_SUPPLIER", "supplier", supplier.supplierId());
        return supplier;
    }

    public Supplier getSupplier(String tenantId, String supplierId) {
        Supplier supplier = requireSupplier(supplierId);
        requireOwnedStore(tenantId, supplier.storeId());
        return supplier;
    }

    public Supplier updateSupplier(String tenantId, String supplierId, UpdateSupplierRequest request) {
        Supplier current = getSupplier(tenantId, supplierId);
        Supplier updated = supplierRepository.save(current.withUpdatedFields(
                request.supplierName(),
                request.sourceUrl(),
                request.priceScore(),
                request.deliveryScore(),
                request.stabilityScore(),
                request.riskLevel(),
                Boolean.TRUE.equals(request.dropshipSupportFlag()),
                Boolean.TRUE.equals(request.blacklistFlag())
        ));
        auditLogService.recordForTenant(tenantId, "UPDATE_SUPPLIER", "supplier", supplierId);
        return updated;
    }

    public Supplier setPrimary(String tenantId, String supplierId) {
        Supplier supplier = getSupplier(tenantId, supplierId);
        List<Supplier> suppliers = supplierRepository.findByStoreId(supplier.storeId());
        for (Supplier current : suppliers) {
            boolean primary = current.supplierId().equals(supplierId);
            supplierRepository.save(current.withPriority(primary, primary ? current.backup() : false));
        }
        Supplier updated = requireSupplier(supplierId);
        auditLogService.recordForTenant(tenantId, "SET_PRIMARY_SUPPLIER", "supplier", supplierId);
        return updated;
    }

    public Supplier setBackup(String tenantId, String supplierId) {
        Supplier supplier = getSupplier(tenantId, supplierId);
        List<Supplier> suppliers = supplierRepository.findByStoreId(supplier.storeId());
        for (Supplier current : suppliers) {
            boolean backup = current.supplierId().equals(supplierId);
            supplierRepository.save(current.withPriority(backup ? current.primary() : false, backup));
        }
        Supplier updated = requireSupplier(supplierId);
        auditLogService.recordForTenant(tenantId, "SET_BACKUP_SUPPLIER", "supplier", supplierId);
        return updated;
    }

    public SupplierAdmissionReviewView reviewAdmission(String tenantId, String supplierId, SaveSupplierAdmissionReviewRequest request) {
        Supplier supplier = getSupplier(tenantId, supplierId);
        List<String> qualificationDocs = request.qualificationDocs() == null ? List.of() : request.qualificationDocs().stream()
                .filter(item -> item != null && !item.isBlank())
                .toList();
        boolean qualificationComplete = qualificationDocs.size() >= 3;
        String admissionStatus = normalizeAdmissionDecision(request.decision());
        SupplierAdmissionReviewView view = new SupplierAdmissionReviewView(
                supplier.supplierId(),
                supplier.storeId(),
                admissionStatus,
                "approved".equals(admissionStatus) && qualificationComplete,
                qualificationComplete,
                qualificationDocs,
                request.remark(),
                OffsetDateTime.now()
        );
        admissionReviews.put(supplierId, view);
        auditLogService.recordForTenant(tenantId, "SUPPLIER_ADMISSION_REVIEW", "supplier", supplierId);
        return view;
    }

    public SupplierScorecardView saveScorecard(String tenantId, String supplierId, SaveSupplierScorecardRequest request) {
        Supplier supplier = getSupplier(tenantId, supplierId);
        BigDecimal compositeScore = BigDecimal.valueOf(request.deliveryScore() + request.fulfillmentScore() + request.qualityScore())
                .divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP);
        SupplierScorecardView view = new SupplierScorecardView(
                supplier.supplierId(),
                supplier.storeId(),
                request.deliveryScore(),
                request.fulfillmentScore(),
                request.qualityScore(),
                compositeScore.doubleValue(),
                deriveRatingGrade(compositeScore.doubleValue()),
                OffsetDateTime.now()
        );
        supplierScorecards.put(supplierId, view);
        auditLogService.recordForTenant(tenantId, "SUPPLIER_SCORECARD", "supplier", supplierId);
        return view;
    }

    public SupplierInquiryView createInquiry(String tenantId, SaveSupplierInquiryRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        if (request.quotes() == null || request.quotes().isEmpty()) {
            throw new BusinessException("7202", "supplier quotes are required", HttpStatus.BAD_REQUEST);
        }
        List<SupplierInquiryQuoteView> normalizedQuotes = new ArrayList<>();
        SupplierInquiryQuoteView recommendedQuote = null;
        for (SupplierInquiryQuoteView quote : request.quotes()) {
            Supplier quoteSupplier = getSupplier(tenantId, quote.supplierId());
            if (!request.storeId().equals(quoteSupplier.storeId())) {
                throw new BusinessException("7203", "quoted suppliers must belong to the same store", HttpStatus.BAD_REQUEST);
            }
            SupplierInquiryQuoteView normalizedQuote = new SupplierInquiryQuoteView(
                    quoteSupplier.supplierId(),
                    quote.quotedUnitPrice(),
                    quote.deliveryDays()
            );
            normalizedQuotes.add(normalizedQuote);
            if (recommendedQuote == null
                    || normalizedQuote.quotedUnitPrice().compareTo(recommendedQuote.quotedUnitPrice()) < 0
                    || (normalizedQuote.quotedUnitPrice().compareTo(recommendedQuote.quotedUnitPrice()) == 0
                    && normalizedQuote.deliveryDays() < recommendedQuote.deliveryDays())) {
                recommendedQuote = normalizedQuote;
            }
        }
        SupplierInquiryView view = new SupplierInquiryView(
                "inquiry-" + inquirySequence++,
                request.storeId(),
                request.productId(),
                request.inquiryTitle(),
                request.targetQty(),
                recommendedQuote.supplierId(),
                normalizedQuotes,
                OffsetDateTime.now()
        );
        supplierInquiries.put(view.inquiryId(), view);
        for (SupplierInquiryQuoteView quote : normalizedQuotes) {
            supplierInquiryIndex.computeIfAbsent(quote.supplierId(), key -> new ArrayList<>()).add(view.inquiryId());
        }
        auditLogService.recordForTenant(tenantId, "SUPPLIER_INQUIRY_CREATE", "supplier_inquiry", view.inquiryId());
        return view;
    }

    public SupplierDeliveryAppointmentView createDeliveryAppointment(String tenantId,
                                                                    String supplierId,
                                                                    SaveSupplierDeliveryAppointmentRequest request) {
        Supplier supplier = getSupplier(tenantId, supplierId);
        requireOwnedStore(tenantId, request.storeId());
        SupplierDeliveryAppointmentView view = new SupplierDeliveryAppointmentView(
                "appointment-" + appointmentSequence++,
                supplier.supplierId(),
                request.storeId(),
                request.purchaseReference(),
                request.appointmentDate(),
                request.plannedQty(),
                "scheduled",
                request.remark(),
                OffsetDateTime.now()
        );
        deliveryAppointments.computeIfAbsent(supplierId, key -> new ArrayList<>()).add(view);
        auditLogService.recordForTenant(tenantId, "SUPPLIER_DELIVERY_APPOINTMENT", "supplier", supplierId);
        return view;
    }

    public SupplierSettlementStatementView createSettlementStatement(String tenantId, SaveSupplierSettlementStatementRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        Supplier supplier = getSupplier(tenantId, request.supplierId());
        SupplierSettlementStatementView view = new SupplierSettlementStatementView(
                "statement-" + settlementSequence++,
                supplier.supplierId(),
                request.storeId(),
                request.statementPeriod(),
                request.accountPeriodDays(),
                request.payableAmount(),
                request.dueDate(),
                "pending",
                OffsetDateTime.now()
        );
        settlementStatements.computeIfAbsent(supplier.supplierId(), key -> new ArrayList<>()).add(view);
        auditLogService.recordForTenant(tenantId, "SUPPLIER_SETTLEMENT_STATEMENT", "supplier_settlement_statement", view.statementId());
        return view;
    }

    public List<SupplierSettlementStatementView> listSettlementStatements(String tenantId, String storeId) {
        requireOwnedStore(tenantId, storeId);
        return settlementStatements.values().stream()
                .flatMap(List::stream)
                .filter(item -> storeId.equals(item.storeId()))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    public SupplierRiskEventView createRiskEvent(String tenantId, String supplierId, SaveSupplierRiskEventRequest request) {
        Supplier supplier = getSupplier(tenantId, supplierId);
        SupplierRiskEventView view = new SupplierRiskEventView(
                "risk-" + riskSequence++,
                supplier.supplierId(),
                supplier.storeId(),
                normalizeSeverity(request.severity()),
                request.riskType(),
                fallbackSupplierId(supplier),
                request.remark(),
                OffsetDateTime.now()
        );
        riskEvents.computeIfAbsent(supplierId, key -> new ArrayList<>()).add(view);
        auditLogService.recordForTenant(tenantId, "SUPPLIER_RISK_EVENT", "supplier", supplierId);
        return view;
    }

    public SupplierSrmLinkageView getSrmLinkage(String tenantId, String supplierId) {
        Supplier supplier = getSupplier(tenantId, supplierId);
        List<String> inquiryIds = supplierInquiryIndex.getOrDefault(supplierId, List.of());
        String latestInquiryId = inquiryIds.isEmpty() ? null : inquiryIds.get(inquiryIds.size() - 1);
        SupplierAdmissionReviewView admissionReview = admissionReviews.get(supplierId);
        SupplierScorecardView scorecard = supplierScorecards.get(supplierId);
        return new SupplierSrmLinkageView(
                supplier.supplierId(),
                supplier.storeId(),
                admissionReview == null ? "pending" : admissionReview.admissionStatus(),
                scorecard == null ? "unrated" : scorecard.ratingGrade(),
                inquiryIds.size(),
                deliveryAppointments.getOrDefault(supplierId, List.of()).size(),
                settlementStatements.getOrDefault(supplierId, List.of()).size(),
                riskEvents.getOrDefault(supplierId, List.of()).size(),
                latestInquiryId,
                "ready",
                "pending"
        );
    }

    public void clear() {
        admissionReviews.clear();
        supplierScorecards.clear();
        supplierInquiries.clear();
        supplierInquiryIndex.clear();
        deliveryAppointments.clear();
        settlementStatements.clear();
        riskEvents.clear();
        inquirySequence = 1;
        appointmentSequence = 1;
        settlementSequence = 1;
        riskSequence = 1;
        supplierRepository.deleteAll();
    }

    private Supplier requireSupplier(String supplierId) {
        return supplierRepository.findBySupplierId(supplierId)
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

    private String normalizeAdmissionDecision(String decision) {
        String normalized = decision == null ? "pending" : decision.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "approved", "rejected", "pending" -> normalized;
            default -> "pending";
        };
    }

    private String deriveRatingGrade(double compositeScore) {
        if (compositeScore >= 90) {
            return "A";
        }
        if (compositeScore >= 80) {
            return "B";
        }
        if (compositeScore >= 70) {
            return "C";
        }
        return "D";
    }

    private String normalizeSeverity(String severity) {
        String normalized = severity == null ? "medium" : severity.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "low", "medium", "high" -> normalized;
            default -> "medium";
        };
    }

    private String fallbackSupplierId(Supplier supplier) {
        return supplierRepository.findByStoreId(supplier.storeId()).stream()
                .filter(candidate -> !candidate.supplierId().equals(supplier.supplierId()))
                .sorted((left, right) -> Boolean.compare(!left.backup(), !right.backup()))
                .map(Supplier::supplierId)
                .findFirst()
                .orElse(supplier.supplierId());
    }

    public record SaveSupplierAdmissionReviewRequest(
            List<String> qualificationDocs,
            String decision,
            String remark
    ) {
    }

    public record SupplierAdmissionReviewView(
            String supplierId,
            String storeId,
            String admissionStatus,
            boolean whitelistFlag,
            boolean qualificationComplete,
            List<String> qualificationDocs,
            String remark,
            OffsetDateTime reviewedAt
    ) {
    }

    public record SaveSupplierScorecardRequest(
            Integer deliveryScore,
            Integer fulfillmentScore,
            Integer qualityScore
    ) {
    }

    public record SupplierScorecardView(
            String supplierId,
            String storeId,
            Integer deliveryScore,
            Integer fulfillmentScore,
            Integer qualityScore,
            double compositeScore,
            String ratingGrade,
            OffsetDateTime scoredAt
    ) {
    }

    public record SaveSupplierInquiryRequest(
            String storeId,
            String productId,
            String inquiryTitle,
            Integer targetQty,
            List<SupplierInquiryQuoteView> quotes
    ) {
    }

    public record SupplierInquiryQuoteView(
            String supplierId,
            BigDecimal quotedUnitPrice,
            Integer deliveryDays
    ) {
    }

    public record SupplierInquiryView(
            String inquiryId,
            String storeId,
            String productId,
            String inquiryTitle,
            Integer targetQty,
            String recommendedSupplierId,
            List<SupplierInquiryQuoteView> quotes,
            OffsetDateTime createdAt
    ) {
    }

    public record SaveSupplierDeliveryAppointmentRequest(
            String storeId,
            String purchaseReference,
            LocalDate appointmentDate,
            Integer plannedQty,
            String remark
    ) {
    }

    public record SupplierDeliveryAppointmentView(
            String appointmentId,
            String supplierId,
            String storeId,
            String purchaseReference,
            LocalDate appointmentDate,
            Integer plannedQty,
            String appointmentStatus,
            String remark,
            OffsetDateTime createdAt
    ) {
    }

    public record SaveSupplierSettlementStatementRequest(
            String storeId,
            String supplierId,
            String statementPeriod,
            Integer accountPeriodDays,
            BigDecimal payableAmount,
            LocalDate dueDate
    ) {
    }

    public record SupplierSettlementStatementView(
            String statementId,
            String supplierId,
            String storeId,
            String statementPeriod,
            Integer accountPeriodDays,
            BigDecimal payableAmount,
            LocalDate dueDate,
            String settlementStatus,
            OffsetDateTime createdAt
    ) {
    }

    public record SaveSupplierRiskEventRequest(
            String riskType,
            String severity,
            String remark
    ) {
    }

    public record SupplierRiskEventView(
            String riskEventId,
            String supplierId,
            String storeId,
            String warningLevel,
            String riskType,
            String fallbackSupplierId,
            String remark,
            OffsetDateTime createdAt
    ) {
    }

    public record SupplierSrmLinkageView(
            String supplierId,
            String storeId,
            String admissionStatus,
            String ratingGrade,
            int inquiryCount,
            int deliveryAppointmentCount,
            int settlementStatementCount,
            int riskEventCount,
            String latestInquiryId,
            String erpSyncStatus,
            String wmsInboundStatus
    ) {
    }
}

