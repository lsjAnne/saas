package backend.member.application;

import backend.audit.application.AuditLogService;
import backend.campaign.domain.repository.CampaignActivityRepository;
import backend.campaign.model.CampaignActivity;
import backend.common.exception.BusinessException;
import backend.member.domain.repository.MemberProfileRepository;
import backend.member.domain.repository.MemberTagRepository;
import backend.member.model.MemberProfile;
import backend.member.model.MemberTag;
import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderMain;
import backend.servicecase.application.ServiceCaseService;
import backend.servicecase.model.AfterSaleRecord;
import backend.servicecase.model.CustomerServiceTicket;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private static final List<String> ALLOWED_TAG_SOURCE_TYPES = List.of("rule", "manual", "ai");
    private static final List<String> ALLOWED_CUSTOMER_TIERS = List.of("standard", "vip", "strategic", "key_account");
    private static final List<String> ALLOWED_TOUCH_TASK_TYPES = List.of("repurchase", "recall", "care");
    private static final List<String> ALLOWED_TOUCH_TRIGGER_TYPES = List.of("manual", "segment_rule", "system");
    private static final List<String> ALLOWED_TOUCH_CHANNELS = List.of("site_message", "sms", "wechat");
    private static final List<String> ACTIVE_TOUCH_TASK_STATUSES = List.of("planned", "in_progress");

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberTagRepository memberTagRepository;
    private final CampaignActivityRepository campaignActivityRepository;
    private final ServiceCaseService serviceCaseService;
    private final Map<String, MemberCrmProfileView> crmProfiles = new LinkedHashMap<>();
    private final Map<String, MemberSegmentRuleView> segmentRules = new LinkedHashMap<>();
    private final Map<String, MemberTouchTaskView> touchTasks = new LinkedHashMap<>();
    private int segmentRuleSequence = 1;
    private int touchTaskSequence = 1;

    public MemberService(AuditLogService auditLogService,
                         StoreRepository storeRepository,
                         OrderRepository orderRepository,
                         MemberProfileRepository memberProfileRepository,
                         MemberTagRepository memberTagRepository,
                         CampaignActivityRepository campaignActivityRepository,
                         ServiceCaseService serviceCaseService) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.orderRepository = orderRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.memberTagRepository = memberTagRepository;
        this.campaignActivityRepository = campaignActivityRepository;
        this.serviceCaseService = serviceCaseService;
    }

    public MemberPageResult listMembers(String tenantId, MemberQuery query) {
        List<MemberView> filtered = allMemberViews(tenantId, query.storeId()).stream()
                .filter(view -> query.levelCode() == null || query.levelCode().isBlank() || query.levelCode().equals(view.levelCode()))
                .filter(view -> query.tagCode() == null || query.tagCode().isBlank() || view.tags().stream().anyMatch(tag -> query.tagCode().equals(tag.tagCode())))
                .filter(view -> query.lifecycleStage() == null || query.lifecycleStage().isBlank() || query.lifecycleStage().equals(view.lifecycleStage()))
                .toList();
        int page = normalizePage(query.page(), 1);
        int pageSize = normalizePage(query.pageSize(), 20);
        int fromIndex = Math.min((page - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return new MemberPageResult(filtered.subList(fromIndex, toIndex), page, pageSize, filtered.size());
    }

    public MemberDetailView getMember(String tenantId, String memberId) {
        refreshMembersFromOrders(tenantId);
        MemberProfile memberProfile = requireOwnedMember(tenantId, memberId);
        return new MemberDetailView(toView(memberProfile, memberTagRepository.findByMemberId(memberId)));
    }

    public MemberTag addTag(String tenantId, String memberId, SaveMemberTagRequest request) {
        refreshMembersFromOrders(tenantId);
        MemberProfile memberProfile = requireOwnedMember(tenantId, memberId);
        MemberTag saved = upsertTag(
                tenantId,
                memberProfile.storeId(),
                memberId,
                request.tagCode(),
                request.tagName(),
                request.sourceType()
        );
        auditLogService.recordForTenant(tenantId, "ADD_MEMBER_TAG", "member_tag", saved.memberTagId());
        return saved;
    }

    public MemberTag removeTag(String tenantId, String memberId, String memberTagId) {
        refreshMembersFromOrders(tenantId);
        MemberProfile memberProfile = requireOwnedMember(tenantId, memberId);
        MemberTag memberTag = memberTagRepository.findByMemberTagId(memberTagId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!memberId.equals(memberTag.memberId()) || !memberProfile.storeId().equals(memberTag.storeId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        memberTagRepository.deleteByMemberTagId(memberTagId);
        auditLogService.recordForTenant(tenantId, "REMOVE_MEMBER_TAG", "member_tag", memberTagId);
        return memberTag;
    }

    public List<MemberTagSummaryView> listMemberTags(String tenantId, String storeId) {
        refreshMembersFromOrders(tenantId);
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        Map<String, Long> tagCountMap = memberTagRepository.findByStoreIds(storeIds).stream()
                .collect(Collectors.groupingBy(tag -> tag.tagCode() + "||" + tag.tagName() + "||" + tag.sourceType(), Collectors.counting()));
        return tagCountMap.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|\\|", 3);
                    return new MemberTagSummaryView(parts[0], parts[1], parts[2], entry.getValue().intValue());
                })
                .sorted((left, right) -> Integer.compare(right.memberCount(), left.memberCount()))
                .toList();
    }

    public MemberGroupExportView exportMemberGroup(String tenantId, ExportMemberGroupRequest request) {
        MemberPageResult pageResult = listMembers(tenantId, new MemberQuery(
                request.storeId(),
                request.levelCode(),
                request.tagCode(),
                request.lifecycleStage(),
                1,
                1000
        ));
        List<String> memberIds = pageResult.list().stream()
                .map(MemberView::memberId)
                .toList();
        auditLogService.recordForTenant(tenantId, "EXPORT_MEMBER_GROUP", "member_profile", "group");
        return new MemberGroupExportView(
                "member-export-" + System.currentTimeMillis(),
                request.storeId(),
                request.levelCode(),
                request.tagCode(),
                request.lifecycleStage(),
                memberIds.size(),
                memberIds
        );
    }

    public List<MemberProfile> listAllProfiles(String tenantId, String storeId) {
        refreshMembersFromOrders(tenantId);
        return memberProfileRepository.findByStoreIds(resolveStoreIds(tenantId, storeId));
    }

    public MemberCrmProfileView saveCrmProfile(String tenantId, String memberId, SaveMemberCrmProfileRequest request) {
        MemberProfile memberProfile = requireOwnedMember(tenantId, memberId);
        MemberCrmProfileView profile = new MemberCrmProfileView(
                memberId,
                memberProfile.storeId(),
                memberProfile.customerId(),
                fallback(request.customerName(), memberProfile.nickname()),
                request.primaryContactName(),
                request.primaryContactMobile(),
                request.wechatId(),
                request.sourceChannel(),
                request.sourceDetail(),
                normalizeCustomerTier(request.customerTier()),
                OffsetDateTime.now()
        );
        crmProfiles.put(memberId, profile);
        auditLogService.recordForTenant(tenantId, "SAVE_MEMBER_CRM_PROFILE", "member_profile", memberId);
        return profile;
    }

    public MemberSegmentRuleView createSegmentRule(String tenantId, SaveMemberSegmentRuleRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        if (request.tagCode() == null || request.tagCode().isBlank() || request.tagName() == null || request.tagName().isBlank()) {
            throw new BusinessException("7501", "segment rule tagCode and tagName are required", HttpStatus.BAD_REQUEST);
        }
        MemberSegmentRuleView rule = new MemberSegmentRuleView(
                "member-segment-rule-" + segmentRuleSequence++,
                request.storeId(),
                request.ruleName(),
                blankToNull(request.lifecycleStage()),
                request.minTotalPaidAmount(),
                blankToNull(request.levelCode()),
                request.tagCode(),
                request.tagName(),
                OffsetDateTime.now()
        );
        segmentRules.put(rule.ruleId(), rule);
        auditLogService.recordForTenant(tenantId, "CREATE_MEMBER_SEGMENT_RULE", "member_segment_rule", rule.ruleId());
        return rule;
    }

    public MemberSegmentExecutionView executeSegmentRule(String tenantId, String ruleId) {
        MemberSegmentRuleView rule = requireOwnedSegmentRule(tenantId, ruleId);
        List<MemberView> matchedMembers = allMemberViews(tenantId, rule.storeId()).stream()
                .filter(member -> matchesSegmentRule(rule, member))
                .toList();
        for (MemberView member : matchedMembers) {
            upsertTag(tenantId, member.storeId(), member.memberId(), rule.tagCode(), rule.tagName(), "rule");
        }
        auditLogService.recordForTenant(tenantId, "EXECUTE_MEMBER_SEGMENT_RULE", "member_segment_rule", ruleId);
        return new MemberSegmentExecutionView(
                rule.ruleId(),
                rule.ruleName(),
                matchedMembers.size(),
                matchedMembers.stream().map(MemberView::memberId).toList()
        );
    }

    public MemberTouchTaskView createTouchTask(String tenantId, SaveMemberTouchTaskRequest request) {
        MemberProfile memberProfile = requireOwnedMember(tenantId, request.memberId());
        if (!memberProfile.storeId().equals(request.storeId())) {
            throw new BusinessException("7502", "touch task storeId does not match member store", HttpStatus.BAD_REQUEST);
        }
        String campaignId = blankToNull(request.campaignId());
        if (campaignId != null) {
            CampaignActivity campaignActivity = campaignActivityRepository.findByCampaignId(campaignId)
                    .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
            if (!request.storeId().equals(campaignActivity.storeId())) {
                throw new BusinessException("7503", "touch task campaign does not belong to member store", HttpStatus.BAD_REQUEST);
            }
        }
        MemberTouchTaskView task = new MemberTouchTaskView(
                "member-touch-task-" + touchTaskSequence++,
                request.storeId(),
                request.memberId(),
                normalizeTouchTaskType(request.taskType()),
                normalizeTouchTriggerType(request.triggerType()),
                campaignId,
                normalizeTouchChannel(request.channel()),
                "planned",
                request.scheduledAt() == null ? OffsetDateTime.now() : request.scheduledAt(),
                request.remark(),
                OffsetDateTime.now()
        );
        touchTasks.put(task.taskId(), task);
        auditLogService.recordForTenant(tenantId, "CREATE_MEMBER_TOUCH_TASK", "member_touch_task", task.taskId());
        return task;
    }

    public List<MemberTouchTaskView> listTouchTasks(String tenantId, TouchTaskQuery query) {
        List<String> storeScope = query.storeId() == null || query.storeId().isBlank()
                ? ownedStoreIds(tenantId)
                : resolveStoreIds(tenantId, query.storeId());
        if (query.memberId() != null && !query.memberId().isBlank()) {
            MemberProfile memberProfile = requireOwnedMember(tenantId, query.memberId());
            storeScope = List.of(memberProfile.storeId());
        }
        List<String> finalStoreScope = storeScope;
        return touchTasks.values().stream()
                .filter(task -> finalStoreScope.contains(task.storeId()))
                .filter(task -> query.memberId() == null || query.memberId().isBlank() || query.memberId().equals(task.memberId()))
                .filter(task -> query.taskType() == null || query.taskType().isBlank() || query.taskType().equals(task.taskType()))
                .filter(task -> query.status() == null || query.status().isBlank() || query.status().equals(task.status()))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    public MemberCrmAnalysisView getCrmAnalysis(String tenantId, String storeId) {
        List<MemberView> members = allMemberViews(tenantId, storeId);
        List<String> storeScope = resolveStoreIds(tenantId, storeId);
        int strategicMembers = (int) members.stream()
                .map(MemberView::memberId)
                .map(crmProfiles::get)
                .filter(Objects::nonNull)
                .filter(profile -> "strategic".equals(profile.customerTier()))
                .count();
        int highValueMembers = (int) members.stream()
                .filter(member -> isHighValueMember(member.totalPaidAmount()))
                .count();
        int dormantMembers = (int) members.stream()
                .filter(member -> "dormant".equals(member.lifecycleStage()))
                .count();
        int silentMembers = (int) members.stream()
                .filter(member -> isSilentMember(member.lastOrderAt()))
                .count();
        int churnWarningMembers = (int) members.stream()
                .filter(member -> !"none".equals(buildChurnWarningLevel(member)))
                .count();
        int autoTaggedMembers = (int) members.stream()
                .filter(member -> member.tags().stream().anyMatch(tag -> "rule".equals(tag.sourceType())))
                .count();
        int activeRecallTaskCount = (int) touchTasks.values().stream()
                .filter(task -> storeScope.contains(task.storeId()))
                .filter(task -> "recall".equals(task.taskType()))
                .filter(task -> ACTIVE_TOUCH_TASK_STATUSES.contains(task.status()))
                .count();
        return new MemberCrmAnalysisView(
                members.size(),
                strategicMembers,
                highValueMembers,
                dormantMembers,
                silentMembers,
                churnWarningMembers,
                autoTaggedMembers,
                activeRecallTaskCount
        );
    }

    public MemberCrmLinkageView getCrmLinkage(String tenantId, String memberId) {
        MemberProfile memberProfile = requireOwnedMember(tenantId, memberId);
        MemberView memberView = toView(memberProfile, memberTagRepository.findByMemberId(memberId));
        MemberCrmProfileView crmProfile = crmProfiles.get(memberId);
        List<OrderMain> relatedOrders = orderRepository.findByStoreIds(List.of(memberProfile.storeId())).stream()
                .filter(order -> memberView.customerId().equals(buildCustomerId(order)))
                .toList();
        List<String> relatedOrderIds = relatedOrders.stream()
                .map(OrderMain::orderId)
                .toList();
        List<CustomerServiceTicket> relatedTickets = serviceCaseService.listTicketsByCustomer(
                tenantId,
                memberProfile.storeId(),
                memberView.customerId()
        );
        List<AfterSaleRecord> relatedAfterSales = serviceCaseService.listAfterSalesByOrderIds(tenantId, relatedOrderIds);
        List<MemberTouchTaskView> relatedTouchTasks = touchTasks.values().stream()
                .filter(task -> memberId.equals(task.memberId()))
                .toList();
        int relatedCampaignCount = (int) relatedTouchTasks.stream()
                .map(MemberTouchTaskView::campaignId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        int activeTouchTaskCount = (int) relatedTouchTasks.stream()
                .filter(task -> ACTIVE_TOUCH_TASK_STATUSES.contains(task.status()))
                .count();
        return new MemberCrmLinkageView(
                memberId,
                memberProfile.storeId(),
                memberView.customerId(),
                crmProfile == null ? memberView.nickname() : crmProfile.customerName(),
                crmProfile == null ? null : crmProfile.primaryContactName(),
                crmProfile == null ? null : crmProfile.sourceChannel(),
                crmProfile == null ? "standard" : crmProfile.customerTier(),
                memberView.lifecycleStage(),
                memberView.totalPaidAmount(),
                memberView.lastOrderAt(),
                relatedOrders.size(),
                relatedAfterSales.size(),
                relatedTickets.size(),
                relatedCampaignCount,
                activeTouchTaskCount,
                buildChurnWarningLevel(memberView),
                memberView.tags().stream().map(MemberTagItemView::tagCode).toList()
        );
    }

    public void clear() {
        memberTagRepository.deleteAll();
        memberProfileRepository.deleteAll();
        crmProfiles.clear();
        segmentRules.clear();
        touchTasks.clear();
        segmentRuleSequence = 1;
        touchTaskSequence = 1;
    }

    private List<MemberView> allMemberViews(String tenantId, String storeId) {
        refreshMembersFromOrders(tenantId);
        List<MemberProfile> members = memberProfileRepository.findByStoreIds(resolveStoreIds(tenantId, storeId));
        Map<String, List<MemberTag>> tagMap = memberProfileTags(members);
        return members.stream()
                .map(member -> toView(member, tagMap.getOrDefault(member.memberId(), List.of())))
                .sorted((left, right) -> {
                    int compareAmount = right.totalPaidAmount().compareTo(left.totalPaidAmount());
                    if (compareAmount != 0) {
                        return compareAmount;
                    }
                    return right.totalOrderCount() - left.totalOrderCount();
                })
                .toList();
    }

    private void refreshMembersFromOrders(String tenantId) {
        Map<String, MemberProfileAggregation> aggregations = new LinkedHashMap<>();
        for (OrderMain order : orderRepository.findByStoreIds(ownedStoreIds(tenantId))) {
            String customerId = buildCustomerId(order);
            String key = order.storeId() + "::" + customerId;
            MemberProfileAggregation current = aggregations.get(key);
            if (current == null) {
                aggregations.put(key, new MemberProfileAggregation(
                        order.storeId(),
                        customerId,
                        order.buyerName(),
                        1,
                        order.totalAmount(),
                        order.createdAt()
                ));
                continue;
            }
            aggregations.put(key, current.merge(order.totalAmount(), order.createdAt()));
        }

        for (MemberProfileAggregation aggregation : aggregations.values()) {
            String levelCode = deriveLevelCode(aggregation.totalPaidAmount());
            MemberProfile existing = memberProfileRepository.findByStoreAndCustomerId(aggregation.storeId(), aggregation.customerId())
                    .orElse(null);
            memberProfileRepository.save(new MemberProfile(
                    existing == null ? null : existing.memberId(),
                    aggregation.storeId(),
                    aggregation.customerId(),
                    aggregation.nickname(),
                    levelCode,
                    aggregation.totalOrderCount(),
                    aggregation.totalPaidAmount(),
                    aggregation.lastOrderAt(),
                    existing == null ? OffsetDateTime.now() : existing.createdAt()
            ));
        }
    }

    private Map<String, List<MemberTag>> memberProfileTags(List<MemberProfile> members) {
        Map<String, List<MemberTag>> tagMap = memberTagRepository.findByStoreIds(
                        members.stream().map(MemberProfile::storeId).distinct().toList())
                .stream()
                .collect(Collectors.groupingBy(MemberTag::memberId));
        for (MemberProfile member : members) {
            tagMap.putIfAbsent(member.memberId(), List.of());
        }
        return tagMap;
    }

    private MemberProfile requireOwnedMember(String tenantId, String memberId) {
        refreshMembersFromOrders(tenantId);
        MemberProfile memberProfile = memberProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, memberProfile.storeId());
        return memberProfile;
    }

    private MemberSegmentRuleView requireOwnedSegmentRule(String tenantId, String ruleId) {
        MemberSegmentRuleView rule = segmentRules.get(ruleId);
        if (rule == null) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        requireOwnedStore(tenantId, rule.storeId());
        return rule;
    }

    private MemberTag upsertTag(String tenantId,
                                String storeId,
                                String memberId,
                                String tagCode,
                                String tagName,
                                String sourceType) {
        String normalizedSourceType = normalizeTagSourceType(sourceType);
        MemberTag existing = memberTagRepository.findByMemberId(memberId).stream()
                .filter(tag -> tagCode.equals(tag.tagCode()))
                .findFirst()
                .orElse(null);
        MemberTag saved = memberTagRepository.save(new MemberTag(
                existing == null ? null : existing.memberTagId(),
                storeId,
                memberId,
                tagCode,
                tagName,
                normalizedSourceType,
                existing == null ? OffsetDateTime.now() : existing.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "UPSERT_MEMBER_TAG", "member_tag", saved.memberTagId());
        return saved;
    }

    private boolean matchesSegmentRule(MemberSegmentRuleView rule, MemberView member) {
        if (rule.lifecycleStage() != null && !rule.lifecycleStage().equals(member.lifecycleStage())) {
            return false;
        }
        if (rule.levelCode() != null && !rule.levelCode().equals(member.levelCode())) {
            return false;
        }
        return rule.minTotalPaidAmount() == null || member.totalPaidAmount().compareTo(rule.minTotalPaidAmount()) >= 0;
    }

    private MemberView toView(MemberProfile memberProfile, List<MemberTag> memberTags) {
        List<MemberTagItemView> tags = memberTags.stream()
                .map(tag -> new MemberTagItemView(tag.memberTagId(), tag.tagCode(), tag.tagName(), tag.sourceType()))
                .toList();
        return new MemberView(
                memberProfile.memberId(),
                memberProfile.storeId(),
                memberProfile.customerId(),
                memberProfile.nickname(),
                memberProfile.levelCode(),
                memberProfile.totalOrderCount(),
                memberProfile.totalPaidAmount(),
                memberProfile.lastOrderAt(),
                calculatePoints(memberProfile.totalPaidAmount()),
                calculateGrowthValue(memberProfile.totalOrderCount(), memberProfile.totalPaidAmount()),
                deriveLifecycleStage(memberProfile.lastOrderAt()),
                tags
        );
    }

    private List<String> resolveStoreIds(String tenantId, String storeId) {
        if (storeId != null && !storeId.isBlank()) {
            requireOwnedStore(tenantId, storeId);
            return List.of(storeId);
        }
        return ownedStoreIds(tenantId);
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return store;
    }

    private String normalizeTagSourceType(String sourceType) {
        String normalized = sourceType == null || sourceType.isBlank()
                ? "manual"
                : sourceType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TAG_SOURCE_TYPES.contains(normalized)) {
            throw new BusinessException("7301", "unsupported member tag sourceType", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeCustomerTier(String customerTier) {
        String normalized = customerTier == null || customerTier.isBlank()
                ? "standard"
                : customerTier.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CUSTOMER_TIERS.contains(normalized)) {
            throw new BusinessException("7504", "unsupported customer tier", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeTouchTaskType(String taskType) {
        String normalized = taskType == null ? "" : taskType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TOUCH_TASK_TYPES.contains(normalized)) {
            throw new BusinessException("7505", "unsupported touch task type", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeTouchTriggerType(String triggerType) {
        String normalized = triggerType == null ? "" : triggerType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TOUCH_TRIGGER_TYPES.contains(normalized)) {
            throw new BusinessException("7506", "unsupported touch trigger type", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeTouchChannel(String channel) {
        String normalized = channel == null ? "" : channel.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TOUCH_CHANNELS.contains(normalized)) {
            throw new BusinessException("7507", "unsupported touch channel", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private int normalizePage(Integer rawValue, int defaultValue) {
        if (rawValue == null || rawValue <= 0) {
            return defaultValue;
        }
        return rawValue;
    }

    private int buildCustomerHash(String rawValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return Math.abs(HexFormat.of().formatHex(bytes).hashCode());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("member customer hash failed", exception);
        }
    }

    private String buildCustomerId(OrderMain orderMain) {
        String raw = fallback(orderMain.buyerPhoneMask(), "unknown") + "|" + fallback(orderMain.buyerName(), "unknown");
        return "customer-" + buildCustomerHash(raw);
    }

    private String deriveLevelCode(BigDecimal totalPaidAmount) {
        if (totalPaidAmount.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            return "svip";
        }
        if (totalPaidAmount.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return "vip";
        }
        return "normal";
    }

    private int calculatePoints(BigDecimal totalPaidAmount) {
        return totalPaidAmount.divide(BigDecimal.TEN, 0, RoundingMode.DOWN).intValue();
    }

    private int calculateGrowthValue(int totalOrderCount, BigDecimal totalPaidAmount) {
        return totalOrderCount * 10 + totalPaidAmount.divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN).intValue();
    }

    private String deriveLifecycleStage(OffsetDateTime lastOrderAt) {
        if (lastOrderAt == null) {
            return "new";
        }
        long days = ChronoUnit.DAYS.between(lastOrderAt.toLocalDate(), OffsetDateTime.now().toLocalDate());
        if (days <= 30) {
            return "active";
        }
        if (days <= 90) {
            return "warm";
        }
        return "dormant";
    }

    private boolean isHighValueMember(BigDecimal totalPaidAmount) {
        return totalPaidAmount.compareTo(BigDecimal.valueOf(5000)) >= 0;
    }

    private boolean isSilentMember(OffsetDateTime lastOrderAt) {
        if (lastOrderAt == null) {
            return false;
        }
        return ChronoUnit.DAYS.between(lastOrderAt.toLocalDate(), OffsetDateTime.now().toLocalDate()) > 60;
    }

    private String buildChurnWarningLevel(MemberView member) {
        if (member.lastOrderAt() == null) {
            return "none";
        }
        long silentDays = ChronoUnit.DAYS.between(member.lastOrderAt().toLocalDate(), OffsetDateTime.now().toLocalDate());
        if (silentDays >= 120 && isHighValueMember(member.totalPaidAmount())) {
            return "high";
        }
        if (silentDays > 90) {
            return "medium";
        }
        if (silentDays > 60) {
            return "low";
        }
        return "none";
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record MemberProfileAggregation(
            String storeId,
            String customerId,
            String nickname,
            int totalOrderCount,
            BigDecimal totalPaidAmount,
            OffsetDateTime lastOrderAt
    ) {
        private MemberProfileAggregation merge(BigDecimal orderAmount, OffsetDateTime orderTime) {
            return new MemberProfileAggregation(
                    storeId,
                    customerId,
                    nickname,
                    totalOrderCount + 1,
                    totalPaidAmount.add(orderAmount),
                    orderTime.isAfter(lastOrderAt) ? orderTime : lastOrderAt
            );
        }
    }

    public record MemberQuery(
            String storeId,
            String levelCode,
            String tagCode,
            String lifecycleStage,
            Integer page,
            Integer pageSize
    ) {
    }

    public record MemberPageResult(
            List<MemberView> list,
            int page,
            int pageSize,
            int total
    ) {
    }

    public record MemberView(
            String memberId,
            String storeId,
            String customerId,
            String nickname,
            String levelCode,
            int totalOrderCount,
            BigDecimal totalPaidAmount,
            OffsetDateTime lastOrderAt,
            int points,
            int growthValue,
            String lifecycleStage,
            List<MemberTagItemView> tags
    ) {
    }

    public record MemberDetailView(
            MemberView member
    ) {
    }

    public record MemberTagItemView(
            String memberTagId,
            String tagCode,
            String tagName,
            String sourceType
    ) {
    }

    public record MemberTagSummaryView(
            String tagCode,
            String tagName,
            String sourceType,
            int memberCount
    ) {
    }

    public record ExportMemberGroupRequest(
            String storeId,
            String levelCode,
            String tagCode,
            String lifecycleStage
    ) {
    }

    public record MemberGroupExportView(
            String exportId,
            String storeId,
            String levelCode,
            String tagCode,
            String lifecycleStage,
            int totalMembers,
            List<String> memberIds
    ) {
    }

    public record SaveMemberTagRequest(
            String tagCode,
            String tagName,
            String sourceType
    ) {
    }

    public record SaveMemberCrmProfileRequest(
            String customerName,
            String primaryContactName,
            String primaryContactMobile,
            String wechatId,
            String sourceChannel,
            String sourceDetail,
            String customerTier
    ) {
    }

    public record MemberCrmProfileView(
            String memberId,
            String storeId,
            String customerId,
            String customerName,
            String primaryContactName,
            String primaryContactMobile,
            String wechatId,
            String sourceChannel,
            String sourceDetail,
            String customerTier,
            OffsetDateTime updatedAt
    ) {
    }

    public record SaveMemberSegmentRuleRequest(
            String storeId,
            String ruleName,
            String lifecycleStage,
            BigDecimal minTotalPaidAmount,
            String levelCode,
            String tagCode,
            String tagName
    ) {
    }

    public record MemberSegmentRuleView(
            String ruleId,
            String storeId,
            String ruleName,
            String lifecycleStage,
            BigDecimal minTotalPaidAmount,
            String levelCode,
            String tagCode,
            String tagName,
            OffsetDateTime createdAt
    ) {
    }

    public record MemberSegmentExecutionView(
            String ruleId,
            String ruleName,
            int matchedMemberCount,
            List<String> matchedMemberIds
    ) {
    }

    public record SaveMemberTouchTaskRequest(
            String storeId,
            String memberId,
            String taskType,
            String triggerType,
            String campaignId,
            String channel,
            OffsetDateTime scheduledAt,
            String remark
    ) {
    }

    public record TouchTaskQuery(
            String storeId,
            String memberId,
            String taskType,
            String status
    ) {
    }

    public record MemberTouchTaskView(
            String taskId,
            String storeId,
            String memberId,
            String taskType,
            String triggerType,
            String campaignId,
            String channel,
            String status,
            OffsetDateTime scheduledAt,
            String remark,
            OffsetDateTime createdAt
    ) {
    }

    public record MemberCrmAnalysisView(
            int totalMembers,
            int strategicMembers,
            int highValueMembers,
            int dormantMembers,
            int silentMembers,
            int churnWarningMembers,
            int autoTaggedMembers,
            int activeRecallTaskCount
    ) {
    }

    public record MemberCrmLinkageView(
            String memberId,
            String storeId,
            String customerId,
            String customerName,
            String primaryContactName,
            String sourceChannel,
            String customerTier,
            String lifecycleStage,
            BigDecimal totalPaidAmount,
            OffsetDateTime lastOrderAt,
            int relatedOrderCount,
            int relatedAfterSaleCount,
            int relatedTicketCount,
            int relatedCampaignCount,
            int activeTouchTaskCount,
            String churnWarningLevel,
            List<String> tagCodes
    ) {
    }
}

