package backend.campaign.application;

import backend.approval.application.ApprovalService;
import backend.campaign.domain.repository.CampaignActivityRepository;
import backend.campaign.domain.repository.CouponTemplateRepository;
import backend.campaign.model.CampaignActivity;
import backend.campaign.model.CouponTemplate;
import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CampaignService {

    private static final List<String> ALLOWED_ACTIVITY_TYPES = List.of(
            "full_reduction",
            "discount",
            "gift",
            "bundle",
            "new_customer"
    );
    private static final List<String> ALLOWED_COUPON_DISCOUNT_TYPES = List.of(
            "amount",
            "percentage"
    );
    private static final List<String> ALLOWED_COUPON_STATUSES = List.of(
            "enabled",
            "disabled"
    );

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final CampaignActivityRepository campaignActivityRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    private final ApprovalService approvalService;

    public CampaignService(AuditLogService auditLogService,
                           StoreRepository storeRepository,
                           CampaignActivityRepository campaignActivityRepository,
                           CouponTemplateRepository couponTemplateRepository,
                           ApprovalService approvalService) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.campaignActivityRepository = campaignActivityRepository;
        this.couponTemplateRepository = couponTemplateRepository;
        this.approvalService = approvalService;
    }

    public List<CampaignActivity> listCampaigns(String tenantId) {
        return campaignActivityRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public CampaignActivity createCampaign(String tenantId, SaveCampaignRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        validateCampaignRequest(request);
        if (request.couponTemplateId() != null && !request.couponTemplateId().isBlank()) {
            requireOwnedCouponTemplate(tenantId, request.couponTemplateId());
        }
        CampaignActivity saved = campaignActivityRepository.save(new CampaignActivity(
                null,
                request.storeId(),
                normalizeActivityType(request.activityType()),
                request.activityName(),
                "draft",
                request.startAt(),
                request.endAt(),
                normalizeProductIds(request.productIds()),
                normalizeRule(request.rule()),
                blankToNull(request.couponTemplateId()),
                true,
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_CAMPAIGN", "campaign_activity", saved.campaignId());
        return saved;
    }

    public CampaignActivity getCampaign(String tenantId, String campaignId) {
        CampaignActivity campaign = campaignActivityRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, campaign.storeId());
        return campaign;
    }

    public CampaignActivity updateCampaign(String tenantId, String campaignId, SaveCampaignRequest request) {
        CampaignActivity current = getCampaign(tenantId, campaignId);
        if (!List.of("draft", "rejected").contains(current.status())) {
            throw new BusinessException("7203", "campaign status does not allow update", HttpStatus.BAD_REQUEST);
        }
        validateCampaignRequest(request);
        if (!current.storeId().equals(request.storeId())) {
            throw new BusinessException("7204", "storeId does not allow change", HttpStatus.BAD_REQUEST);
        }
        if (request.couponTemplateId() != null && !request.couponTemplateId().isBlank()) {
            requireOwnedCouponTemplate(tenantId, request.couponTemplateId());
        }
        CampaignActivity updated = campaignActivityRepository.save(current.withEditableFields(
                normalizeActivityType(request.activityType()),
                request.activityName(),
                request.startAt(),
                request.endAt(),
                normalizeProductIds(request.productIds()),
                normalizeRule(request.rule()),
                blankToNull(request.couponTemplateId()),
                true
        ));
        auditLogService.recordForTenant(tenantId, "UPDATE_CAMPAIGN", "campaign_activity", campaignId);
        return updated;
    }

    public CampaignActivity submitApproval(String tenantId, String operatorId, String campaignId) {
        CampaignActivity current = getCampaign(tenantId, campaignId);
        if (!List.of("draft", "rejected").contains(current.status())) {
            throw new BusinessException("7205", "campaign status does not allow submit approval", HttpStatus.BAD_REQUEST);
        }
        if (current.productIds().isEmpty()) {
            throw new BusinessException("7206", "campaign must bind at least one product before approval", HttpStatus.BAD_REQUEST);
        }
        CampaignActivity updated = campaignActivityRepository.save(current.withStatus("pending_approval"));
        approvalService.createApprovalIfAbsent(
                tenantId,
                operatorId,
                "campaign_publish",
                "campaign_activity",
                campaignId,
                operatorId,
                "campaign publish approval"
        );
        auditLogService.recordForTenant(tenantId, "SUBMIT_CAMPAIGN_APPROVAL", "campaign_activity", campaignId);
        return updated;
    }

    public CampaignActivity publishCampaign(String tenantId, String campaignId) {
        CampaignActivity current = getCampaign(tenantId, campaignId);
        if (!"approved".equals(current.status())) {
            throw new BusinessException("7207", "campaign must be approved before publish", HttpStatus.BAD_REQUEST);
        }
        CampaignActivity updated = campaignActivityRepository.save(current.withStatus("published"));
        auditLogService.recordForTenant(tenantId, "PUBLISH_CAMPAIGN", "campaign_activity", campaignId);
        return updated;
    }

    public List<CouponTemplate> listCouponTemplates(String tenantId) {
        return couponTemplateRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public CouponTemplate createCouponTemplate(String tenantId, SaveCouponTemplateRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        validateCouponTemplateRequest(request);
        CouponTemplate saved = couponTemplateRepository.save(new CouponTemplate(
                null,
                request.storeId(),
                request.templateName(),
                normalizeCouponDiscountType(request.discountType()),
                request.discountValue(),
                request.thresholdAmount(),
                normalizeCouponStatus(request.status()),
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_COUPON_TEMPLATE", "coupon_template", saved.couponTemplateId());
        return saved;
    }

    public void clear() {
        campaignActivityRepository.deleteAll();
        couponTemplateRepository.deleteAll();
    }

    private void validateCampaignRequest(SaveCampaignRequest request) {
        String activityType = normalizeActivityType(request.activityType());
        if (request.endAt() != null && request.endAt().isBefore(request.startAt())) {
            throw new BusinessException("7201", "campaign end time must be after start time", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> rule = normalizeRule(request.rule());
        validateCampaignRule(activityType, rule);
    }

    private void validateCampaignRule(String activityType, Map<String, Object> rule) {
        if ("full_reduction".equals(activityType)) {
            BigDecimal thresholdAmount = readBigDecimal(rule, "thresholdAmount");
            BigDecimal discountAmount = readBigDecimal(rule, "discountAmount");
            if (thresholdAmount == null || discountAmount == null) {
                throw new BusinessException("7202", "full_reduction rule requires thresholdAmount and discountAmount", HttpStatus.BAD_REQUEST);
            }
            if (discountAmount.compareTo(thresholdAmount) >= 0) {
                throw new BusinessException("7202", "discountAmount must be less than thresholdAmount", HttpStatus.BAD_REQUEST);
            }
            return;
        }
        if ("discount".equals(activityType)) {
            BigDecimal discountRate = readBigDecimal(rule, "discountRate");
            if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) <= 0 || discountRate.compareTo(BigDecimal.ONE) > 0) {
                throw new BusinessException("7202", "discount rule requires discountRate between 0 and 1", HttpStatus.BAD_REQUEST);
            }
            return;
        }
        if (rule.isEmpty()) {
            throw new BusinessException("7202", "campaign rule is required", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateCouponTemplateRequest(SaveCouponTemplateRequest request) {
        String discountType = normalizeCouponDiscountType(request.discountType());
        if (request.discountValue() == null || request.discountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("7211", "coupon discountValue must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        if ("percentage".equals(discountType)
                && (request.discountValue().compareTo(BigDecimal.ONE) > 0 || request.discountValue().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException("7211", "coupon percentage discountValue must be between 0 and 1", HttpStatus.BAD_REQUEST);
        }
        if (request.thresholdAmount() != null && request.thresholdAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("7212", "coupon thresholdAmount must be greater than or equal to 0", HttpStatus.BAD_REQUEST);
        }
        normalizeCouponStatus(request.status());
    }

    private CouponTemplate requireOwnedCouponTemplate(String tenantId, String couponTemplateId) {
        CouponTemplate couponTemplate = couponTemplateRepository.findByCouponTemplateId(couponTemplateId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, couponTemplate.storeId());
        if (!"enabled".equals(couponTemplate.status())) {
            throw new BusinessException("7213", "coupon template must be enabled", HttpStatus.BAD_REQUEST);
        }
        return couponTemplate;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private String normalizeActivityType(String activityType) {
        String normalized = activityType == null ? "" : activityType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_ACTIVITY_TYPES.contains(normalized)) {
            throw new BusinessException("7200", "unsupported campaign activityType", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeCouponDiscountType(String discountType) {
        String normalized = discountType == null ? "" : discountType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_COUPON_DISCOUNT_TYPES.contains(normalized)) {
            throw new BusinessException("7210", "unsupported coupon discountType", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeCouponStatus(String status) {
        String normalized = status == null || status.isBlank()
                ? "enabled"
                : status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_COUPON_STATUSES.contains(normalized)) {
            throw new BusinessException("7214", "unsupported coupon status", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private List<String> normalizeProductIds(List<String> productIds) {
        return productIds == null ? List.of() : productIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private Map<String, Object> normalizeRule(Map<String, Object> rule) {
        return rule == null ? Map.of() : rule;
    }

    private BigDecimal readBigDecimal(Map<String, Object> rule, String fieldName) {
        Object value = rule.get(fieldName);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            throw new BusinessException("7202", "campaign rule numeric field is invalid", HttpStatus.BAD_REQUEST);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record SaveCampaignRequest(
            String storeId,
            String activityType,
            String activityName,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            List<String> productIds,
            Map<String, Object> rule,
            String couponTemplateId
    ) {
    }

    public record SaveCouponTemplateRequest(
            String storeId,
            String templateName,
            String discountType,
            BigDecimal discountValue,
            BigDecimal thresholdAmount,
            String status
    ) {
    }
}

