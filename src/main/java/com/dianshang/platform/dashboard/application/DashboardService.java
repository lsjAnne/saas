package com.dianshang.platform.dashboard.application;

import com.dianshang.platform.approval.domain.repository.ApprovalInstanceRepository;
import com.dianshang.platform.campaign.domain.repository.CampaignActivityRepository;
import com.dianshang.platform.campaign.domain.repository.CouponTemplateRepository;
import com.dianshang.platform.campaign.model.CampaignActivity;
import com.dianshang.platform.campaign.model.CouponTemplate;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.exceptioncenter.domain.repository.ExceptionTaskRepository;
import com.dianshang.platform.finance.application.FinanceService;
import com.dianshang.platform.finance.model.FinanceBill;
import com.dianshang.platform.fulfillment.domain.repository.FulfillmentTaskRepository;
import com.dianshang.platform.inventory.domain.repository.InventorySnapshotRepository;
import com.dianshang.platform.inventory.model.InventorySnapshot;
import com.dianshang.platform.member.application.MemberService;
import com.dianshang.platform.member.model.MemberProfile;
import com.dianshang.platform.notification.domain.repository.NotificationTaskRepository;
import com.dianshang.platform.notification.model.NotificationTask;
import com.dianshang.platform.order.domain.repository.OrderRepository;
import com.dianshang.platform.order.model.OrderMain;
import com.dianshang.platform.servicecase.application.ServiceCaseService;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import com.dianshang.platform.supplier.domain.repository.SupplierRepository;
import com.dianshang.platform.supplier.model.Supplier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DashboardService {

    private static final Map<String, BiMetricDefinitionState> BI_METRIC_DEFINITION_STORAGE = new LinkedHashMap<>();
    private static final Map<String, BiExportTaskState> BI_EXPORT_TASK_STORAGE = new LinkedHashMap<>();
    private static final Map<String, BiSubscriptionState> BI_SUBSCRIPTION_STORAGE = new LinkedHashMap<>();
    private static final Map<String, BiRepairTaskState> BI_REPAIR_TASK_STORAGE = new LinkedHashMap<>();
    private static final AtomicLong BI_METRIC_DEFINITION_SEQUENCE = new AtomicLong(1L);
    private static final AtomicLong BI_EXPORT_TASK_SEQUENCE = new AtomicLong(1L);
    private static final AtomicLong BI_SUBSCRIPTION_SEQUENCE = new AtomicLong(1L);
    private static final AtomicLong BI_REPAIR_TASK_SEQUENCE = new AtomicLong(1L);

    private static final int BI_ANALYSIS_WINDOW_DAYS = 30;

    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final ExceptionTaskRepository exceptionTaskRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final CampaignActivityRepository campaignActivityRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final NotificationTaskRepository notificationTaskRepository;
    private final MemberService memberService;
    private final SupplierRepository supplierRepository;
    private final ServiceCaseService serviceCaseService;
    private final FinanceService financeService;
    private final Environment environment;

    public DashboardService(StoreRepository storeRepository,
                            OrderRepository orderRepository,
                            FulfillmentTaskRepository fulfillmentTaskRepository,
                            ExceptionTaskRepository exceptionTaskRepository,
                            InventorySnapshotRepository inventorySnapshotRepository,
                            CampaignActivityRepository campaignActivityRepository,
                            CouponTemplateRepository couponTemplateRepository,
                            ApprovalInstanceRepository approvalInstanceRepository,
                            NotificationTaskRepository notificationTaskRepository,
                            MemberService memberService,
                            SupplierRepository supplierRepository,
                            ServiceCaseService serviceCaseService,
                            FinanceService financeService,
                            Environment environment) {
        this.storeRepository = storeRepository;
        this.orderRepository = orderRepository;
        this.fulfillmentTaskRepository = fulfillmentTaskRepository;
        this.exceptionTaskRepository = exceptionTaskRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.campaignActivityRepository = campaignActivityRepository;
        this.couponTemplateRepository = couponTemplateRepository;
        this.approvalInstanceRepository = approvalInstanceRepository;
        this.notificationTaskRepository = notificationTaskRepository;
        this.memberService = memberService;
        this.supplierRepository = supplierRepository;
        this.serviceCaseService = serviceCaseService;
        this.financeService = financeService;
        this.environment = environment;
    }

    public DashboardSummaryView getSummary(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        LocalDate today = OffsetDateTime.now().toLocalDate();
        List<OrderMain> orders = orderRepository.findByStoreIds(storeIds).stream()
                .filter(order -> today.equals(order.createdAt().toLocalDate()))
                .toList();
        BigDecimal todaySalesAmount = sumOrderAmount(orders);
        int todayOrderCount = orders.size();
        BigDecimal grossProfit = sumEstimatedProfit(orders);
        int exceptionCount = (int) exceptionTaskRepository.findByStoreIds(storeIds).stream()
                .filter(task -> !List.of("resolved", "ignored").contains(task.status()))
                .count();
        int pendingConfirmCount = (int) fulfillmentTaskRepository.findByStoreIds(storeIds).stream()
                .filter(task -> "pending_confirm".equals(task.status()))
                .count();
        int lowStockCount = (int) inventorySnapshotRepository.findByStoreIds(storeIds).stream()
                .filter(this::isLowStock)
                .count();
        String topSuggestion = lowStockCount >= exceptionCount
                ? "建议优先处理低库存与补货提醒"
                : "建议优先处理异常任务与待审批事项";
        return new DashboardSummaryView(
                todaySalesAmount,
                todayOrderCount,
                grossProfit,
                exceptionCount,
                pendingConfirmCount,
                lowStockCount,
                topSuggestion
        );
    }

    public List<DashboardTrendView> getTrends(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        List<OrderMain> orders = orderRepository.findByStoreIds(storeIds);
        List<DashboardTrendView> trends = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = OffsetDateTime.now().toLocalDate().minusDays(i);
            List<OrderMain> dayOrders = orders.stream()
                    .filter(order -> date.equals(order.createdAt().toLocalDate()))
                    .toList();
            trends.add(new DashboardTrendView(
                    date,
                    sumOrderAmount(dayOrders),
                    dayOrders.size(),
                    sumEstimatedProfit(dayOrders)
            ));
        }
        return trends;
    }

    public List<DashboardRiskView> getRisks(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        List<DashboardRiskView> risks = new ArrayList<>();

        long exceptionCount = exceptionTaskRepository.findByStoreIds(storeIds).stream()
                .filter(task -> !List.of("resolved", "ignored").contains(task.status()))
                .count();
        if (exceptionCount > 0) {
            risks.add(new DashboardRiskView(
                    "exception_backlog",
                    "异常积压",
                    "high",
                    (int) exceptionCount,
                    "优先处理未解决的异常任务"
            ));
        }

        long lowStockCount = inventorySnapshotRepository.findByStoreIds(storeIds).stream()
                .filter(this::isLowStock)
                .count();
        if (lowStockCount > 0) {
            risks.add(new DashboardRiskView(
                    "low_stock",
                    "低库存风险",
                    "medium",
                    (int) lowStockCount,
                    "及时补货或调整营销活动库存阈值"
            ));
        }

        long pendingApprovals = approvalInstanceRepository.findByTenantId(tenantId).stream()
                .filter(approval -> "pending".equals(approval.status()))
                .count();
        if (pendingApprovals > 0) {
            risks.add(new DashboardRiskView(
                    "approval_pending",
                    "审批积压",
                    "medium",
                    (int) pendingApprovals,
                    "尽快处理待审批单据"
            ));
        }

        long deadLetters = notificationTaskRepository.findByTenantId(tenantId).stream()
                .filter(task -> "dead_letter".equals(task.sendStatus()))
                .count();
        if (deadLetters > 0) {
            risks.add(new DashboardRiskView(
                    "notification_dead_letter",
                    "通知死信",
                    "medium",
                    (int) deadLetters,
                    "检查通知模板、接收人和重试策略"
            ));
        }

        return risks;
    }

    public DashboardCampaignAnalysisView getCampaignAnalysis(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        List<CampaignActivity> campaigns = campaignActivityRepository.findByStoreIds(storeIds);
        int totalCampaignCount = campaigns.size();
        int publishedCampaignCount = (int) campaigns.stream()
                .filter(item -> "published".equals(item.status()))
                .count();
        int pendingApprovalCount = (int) campaigns.stream()
                .filter(item -> "pending_approval".equals(item.status()))
                .count();
        BigDecimal publishSuccessRate = totalCampaignCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(publishedCampaignCount)
                .divide(BigDecimal.valueOf(totalCampaignCount), 4, RoundingMode.HALF_UP);
        int totalCouponTemplateCount = couponTemplateRepository.findByStoreIds(storeIds).size();
        String recommendedStrategy = pendingApprovalCount > 0
                ? "先清理审批中的活动，再扩大投放范围"
                : publishedCampaignCount > 0
                ? "优先放大已发布活动的人群触达范围"
                : "优先创建新客活动并绑定高毛利商品";
        return new DashboardCampaignAnalysisView(
                totalCampaignCount,
                publishedCampaignCount,
                pendingApprovalCount,
                publishSuccessRate,
                totalCouponTemplateCount,
                recommendedStrategy
        );
    }

    public DashboardMemberAnalysisView getMemberAnalysis(String tenantId, String storeId) {
        List<MemberProfile> members = memberService.listAllProfiles(tenantId, storeId);
        int totalMembers = members.size();
        int vipMembers = (int) members.stream()
                .filter(member -> List.of("vip", "svip").contains(member.levelCode()))
                .count();
        int repurchaseMembers = (int) members.stream()
                .filter(member -> member.totalOrderCount() >= 2)
                .count();
        int dormantMembers = (int) members.stream()
                .filter(member -> isDormantMember(member.lastOrderAt()))
                .count();
        BigDecimal averagePaidAmount = totalMembers == 0
                ? BigDecimal.ZERO
                : members.stream()
                .map(MemberProfile::totalPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totalMembers), 2, RoundingMode.HALF_UP);
        BigDecimal repurchaseRate = totalMembers == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(repurchaseMembers)
                .divide(BigDecimal.valueOf(totalMembers), 4, RoundingMode.HALF_UP);
        return new DashboardMemberAnalysisView(
                totalMembers,
                vipMembers,
                dormantMembers,
                averagePaidAmount,
                repurchaseRate
        );
    }

    public List<BiThemeDomainView> listBiThemeDomains(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        List<OrderMain> orders = orderRepository.findByStoreIds(storeIds);
        List<InventorySnapshot> snapshots = inventorySnapshotRepository.findByStoreIds(storeIds);
        List<Supplier> suppliers = supplierRepository.findByStoreIds(storeIds);
        List<MemberProfile> members = memberService.listAllProfiles(tenantId, storeId);
        List<FinanceBill> financeBills = financeService.listFinanceBills(tenantId).stream()
                .filter(bill -> storeIds.contains(bill.storeId()))
                .toList();
        List<String> orderIds = orders.stream().map(OrderMain::orderId).toList();
        int serviceObjectCount = serviceCaseService.listTickets(tenantId).stream()
                .filter(ticket -> storeIds.contains(ticket.storeId()))
                .toList()
                .size() + serviceCaseService.listAfterSalesByOrderIds(tenantId, orderIds).size();
        return List.of(
                new BiThemeDomainView("order", "订单主题域", "oms", orders.size(), 4, freshnessFromOrders(orders), "dwd"),
                new BiThemeDomainView("product", "商品主题域", "product", (int) snapshots.stream().map(InventorySnapshot::productId).distinct().count(), 3, freshnessFromSnapshots(snapshots), "dws"),
                new BiThemeDomainView("supplier", "供应商主题域", "srm", suppliers.size(), 3, freshnessFromSuppliers(suppliers), "dws"),
                new BiThemeDomainView("member", "会员主题域", "crm", members.size(), 4, freshnessFromMembers(members), "ads"),
                new BiThemeDomainView("service", "客服主题域", "servicecase", serviceObjectCount, 3, serviceObjectCount > 0 ? "fresh" : "empty", "ads"),
                new BiThemeDomainView("finance", "财务主题域", "finance", financeBills.size(), 4, freshnessFromFinanceBills(financeBills), "ads")
        );
    }

    public List<BiMetricDefinitionView> listBiMetricDefinitions(String tenantId) {
        Map<String, BiMetricDefinitionView> merged = new LinkedHashMap<>();
        for (BiMetricDefinitionView builtin : builtinMetricDefinitions(tenantId)) {
            merged.put(builtin.metricCode(), builtin);
        }
        BI_METRIC_DEFINITION_STORAGE.values().stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .sorted(Comparator.comparing(BiMetricDefinitionState::metricCode))
                .map(this::toBiMetricDefinitionView)
                .forEach(item -> merged.put(item.metricCode(), item));
        return merged.values().stream()
                .sorted(Comparator.comparing(BiMetricDefinitionView::metricCategory)
                        .thenComparing(BiMetricDefinitionView::metricCode))
                .toList();
    }

    public BiMetricDefinitionView saveBiMetricDefinition(String tenantId, SaveBiMetricDefinitionRequest request) {
        String metricCode = normalizeRequired(request.metricCode(), "metricCode");
        String key = tenantId + "|" + metricCode;
        BiMetricDefinitionState existing = BI_METRIC_DEFINITION_STORAGE.get(key);
        BiMetricDefinitionState saved = new BiMetricDefinitionState(
                existing == null ? "bi-metric-" + BI_METRIC_DEFINITION_SEQUENCE.getAndIncrement() : existing.metricId(),
                tenantId,
                metricCode,
                normalizeRequired(request.metricName(), "metricName"),
                normalizeRequired(request.metricCategory(), "metricCategory"),
                normalizeRequired(request.metricFormula(), "metricFormula"),
                normalizeRequired(request.metricDescription(), "metricDescription"),
                normalizeRequired(request.ownerDomain(), "ownerDomain"),
                request.enabled() == null || request.enabled(),
                OffsetDateTime.now()
        );
        BI_METRIC_DEFINITION_STORAGE.put(key, saved);
        return toBiMetricDefinitionView(saved);
    }

    public List<BiLayerDefinitionView> listBiLayers() {
        return List.of(
                new BiLayerDefinitionView("ods", "ODS", "订单、履约、营销、客服原始事件", "按源系统增量入湖", "保留原始业务明细，服务回灌与追溯", "active"),
                new BiLayerDefinitionView("dwd", "DWD", "标准化交易与服务事实", "按订单、工单、活动统一主键建模", "沉淀统一明细事实表", "active"),
                new BiLayerDefinitionView("dws", "DWS", "主题汇总指标", "按日/店铺/活动聚合", "输出经营主题域汇总指标", "active"),
                new BiLayerDefinitionView("ads", "ADS", "驾驶舱与专题看板", "按角色与场景组织指标口径", "服务驾驶舱、日报和预警订阅", "active")
        );
    }

    public BiOverviewView getBiOverview(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        List<OrderMain> recentOrders = recentOrders(storeIds, BI_ANALYSIS_WINDOW_DAYS);
        List<MemberProfile> members = memberService.listAllProfiles(tenantId, storeId);
        int repurchaseMembers = (int) members.stream()
                .filter(member -> member.totalOrderCount() >= 2)
                .count();
        BigDecimal repurchaseRate = members.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(repurchaseMembers).divide(BigDecimal.valueOf(members.size()), 4, RoundingMode.HALF_UP);
        int activeCampaignCount = (int) campaignActivityRepository.findByStoreIds(storeIds).stream()
                .filter(this::isActivePublishedCampaign)
                .count();
        int activeMemberCount = (int) members.stream()
                .filter(member -> isRecentWithinDays(member.lastOrderAt(), BI_ANALYSIS_WINDOW_DAYS))
                .count();
        BigDecimal salesIncomeAmount = sumOrderAmount(recentOrders);
        BigDecimal netProfitAmount = sumEstimatedProfit(recentOrders);
        String recommendedAction = repurchaseRate.compareTo(BigDecimal.valueOf(0.40)) < 0
                ? "优先提升复购运营和会员召回"
                : activeCampaignCount == 0
                ? "优先恢复可投放活动，避免经营波峰下滑"
                : "维持高转化活动并观察利润率变化";
        return new BiOverviewView(
                OffsetDateTime.now().toLocalDate().minusDays(BI_ANALYSIS_WINDOW_DAYS - 1L),
                OffsetDateTime.now().toLocalDate(),
                salesIncomeAmount,
                netProfitAmount,
                repurchaseRate,
                activeCampaignCount,
                activeMemberCount,
                recommendedAction
        );
    }

    public BiOperationalAnalysisView getBiOperationalAnalysis(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        int pendingFulfillmentCount = (int) fulfillmentTaskRepository.findByStoreIds(storeIds).stream()
                .filter(task -> "pending_confirm".equals(task.status()))
                .count();
        int overdueFulfillmentCount = (int) fulfillmentTaskRepository.findByStoreIds(storeIds).stream()
                .filter(task -> "pending_confirm".equals(task.status()))
                .filter(task -> task.dueAt() != null && task.dueAt().isBefore(OffsetDateTime.now()))
                .count();
        List<Supplier> suppliers = supplierRepository.findByStoreIds(storeIds);
        int highRiskSupplierCount = (int) suppliers.stream()
                .filter(item -> item.blacklistFlag() || List.of("high", "critical").contains(item.riskLevel()))
                .count();
        ServiceCaseService.TicketSlaOverviewView ticketSlaOverview = serviceCaseService.getTicketSlaOverview(tenantId, storeId);
        String recommendedAction = overdueFulfillmentCount > 0 || ticketSlaOverview.overdueTicketCount() > 0
                ? "优先处理超时履约与客服 SLA 风险"
                : highRiskSupplierCount > 0
                ? "优先切换高风险供应商并审查备份链路"
                : "维持履约与客服稳定节奏，继续压缩异常积压";
        return new BiOperationalAnalysisView(
                pendingFulfillmentCount,
                overdueFulfillmentCount,
                suppliers.size(),
                highRiskSupplierCount,
                ticketSlaOverview.totalTicketCount(),
                ticketSlaOverview.overdueTicketCount(),
                ticketSlaOverview.averageSatisfactionScore(),
                recommendedAction
        );
    }

    public BiMarketingAnalysisView getBiMarketingAnalysis(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        List<CampaignActivity> campaigns = campaignActivityRepository.findByStoreIds(storeIds);
        int totalCampaignCount = campaigns.size();
        int publishedCampaignCount = (int) campaigns.stream()
                .filter(item -> "published".equals(item.status()))
                .count();
        List<OrderMain> recentOrders = recentOrders(storeIds, BI_ANALYSIS_WINDOW_DAYS);
        Map<String, CouponTemplate> couponTemplateMap = couponTemplateRepository.findByStoreIds(storeIds).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.couponTemplateId(), item), Map::putAll);
        BigDecimal recentGrossProfit = sumEstimatedProfit(recentOrders);
        BigDecimal campaignCouponCost = campaigns.stream()
                .filter(this::isActivePublishedCampaign)
                .map(CampaignActivity::couponTemplateId)
                .map(couponTemplateMap::get)
                .filter(item -> item != null)
                .map(CouponTemplate::discountValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal marketingRoiRate = campaignCouponCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : recentGrossProfit.divide(campaignCouponCost, 2, RoundingMode.HALF_UP);
        BigDecimal campaignConversionRate = totalCampaignCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(publishedCampaignCount).divide(BigDecimal.valueOf(totalCampaignCount), 4, RoundingMode.HALF_UP);
        int couponTemplateCount = couponTemplateMap.size();
        int couponBindCampaignCount = (int) campaigns.stream()
                .filter(item -> item.couponTemplateId() != null && !item.couponTemplateId().isBlank())
                .count();
        String recommendedAction = marketingRoiRate.compareTo(BigDecimal.valueOf(1.50)) < 0
                ? "优先压缩低 ROI 活动，收敛优惠券投放"
                : publishedCampaignCount == 0
                ? "优先恢复已验证活动，提升活动转化"
                : "继续放大高 ROI 活动，并复盘优惠券效果";
        return new BiMarketingAnalysisView(
                totalCampaignCount,
                publishedCampaignCount,
                marketingRoiRate,
                campaignConversionRate,
                couponTemplateCount,
                couponBindCampaignCount,
                recommendedAction
        );
    }

    public BiExportTaskView saveBiExportTask(String tenantId,
                                             String operatorId,
                                             SaveBiExportTaskRequest request) {
        String reportCode = normalizeRequired(request.reportCode(), "reportCode");
        String exportFormat = normalizeRequired(request.exportFormat(), "exportFormat");
        if (!List.of("csv", "xlsx", "pdf").contains(exportFormat)) {
            throw new BusinessException("1002", "exportFormat must be csv, xlsx or pdf", HttpStatus.BAD_REQUEST);
        }
        String normalizedStoreId = normalizeOptionalText(request.storeId());
        if (normalizedStoreId != null) {
            requireOwnedStore(tenantId, normalizedStoreId);
        }
        BiExportTaskState saved = new BiExportTaskState(
                "bi-export-" + BI_EXPORT_TASK_SEQUENCE.getAndIncrement(),
                tenantId,
                reportCode,
                normalizedStoreId,
                exportFormat,
                "pending",
                operatorId,
                normalizeOptionalText(request.remark()),
                OffsetDateTime.now()
        );
        BI_EXPORT_TASK_STORAGE.put(saved.taskId(), saved);
        return toBiExportTaskView(saved);
    }

    public List<BiExportTaskView> listBiExportTasks(String tenantId) {
        return BI_EXPORT_TASK_STORAGE.values().stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .sorted(Comparator.comparing(BiExportTaskState::createdAt).reversed())
                .map(this::toBiExportTaskView)
                .toList();
    }

    public BiCockpitView getBiCockpit(String tenantId, String storeId) {
        BiOverviewView overview = getBiOverview(tenantId, storeId);
        List<BiDataQualityCheckView> qualityChecks = listBiDataQualityChecks(tenantId, storeId);
        List<String> alertItems = qualityChecks.stream()
                .filter(item -> !"healthy".equals(item.checkStatus()))
                .map(item -> item.checkName() + "：" + item.affectedCount())
                .toList();
        String headline = "近 30 天销售额 " + overview.salesIncomeAmount().setScale(0, RoundingMode.HALF_UP).toPlainString()
                + "，净利润 " + overview.netProfitAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
        String dailyBrief = "今日建议：" + overview.recommendedAction();
        String weeklyBrief = alertItems.isEmpty()
                ? "周报无阻塞风险，可继续推进活动和复购经营"
                : "周报存在 " + alertItems.size() + " 个治理提醒，建议优先清理异常项";
        return new BiCockpitView(
                headline,
                dailyBrief,
                weeklyBrief,
                alertItems.size(),
                alertItems,
                List.of("bi_cockpit_daily", "bi_profit_weekly", "bi_marketing_alert")
        );
    }

    public BiSubscriptionView saveBiSubscription(String tenantId,
                                                 String operatorId,
                                                 SaveBiSubscriptionRequest request) {
        String subscriptionType = normalizeRequired(request.subscriptionType(), "subscriptionType");
        String reportCode = normalizeRequired(request.reportCode(), "reportCode");
        String scheduleType = normalizeRequired(request.scheduleType(), "scheduleType");
        String channel = normalizeRequired(request.channel(), "channel");
        String recipient = normalizeRequired(request.recipient(), "recipient");
        BiSubscriptionState saved = new BiSubscriptionState(
                "bi-subscription-" + BI_SUBSCRIPTION_SEQUENCE.getAndIncrement(),
                tenantId,
                subscriptionType,
                reportCode,
                scheduleType,
                channel,
                recipient,
                request.enabled() == null || request.enabled(),
                operatorId,
                normalizeOptionalText(request.remark()),
                OffsetDateTime.now()
        );
        BI_SUBSCRIPTION_STORAGE.put(saved.subscriptionId(), saved);
        return toBiSubscriptionView(saved);
    }

    public List<BiSubscriptionView> listBiSubscriptions(String tenantId) {
        return BI_SUBSCRIPTION_STORAGE.values().stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .sorted(Comparator.comparing(BiSubscriptionState::createdAt).reversed())
                .map(this::toBiSubscriptionView)
                .toList();
    }

    public List<BiDataQualityCheckView> listBiDataQualityChecks(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        List<OrderMain> orders = orderRepository.findByStoreIds(storeIds);
        int orderWithoutProfitCount = (int) orders.stream()
                .filter(item -> item.estimatedProfit() == null || item.estimatedProfit().compareTo(BigDecimal.ZERO) <= 0)
                .count();
        int lowStockCount = (int) inventorySnapshotRepository.findByStoreIds(storeIds).stream()
                .filter(this::isLowStock)
                .count();
        int pendingApprovalCount = (int) approvalInstanceRepository.findByTenantId(tenantId).stream()
                .filter(item -> "pending".equals(item.status()))
                .count();
        int deadLetterCount = (int) notificationTaskRepository.findByTenantId(tenantId).stream()
                .filter(item -> "dead_letter".equals(item.sendStatus()))
                .count();
        int dormantMemberCount = (int) memberService.listAllProfiles(tenantId, storeId).stream()
                .filter(member -> isDormantMember(member.lastOrderAt()))
                .count();
        return List.of(
                buildQualityCheck("order_missing_profit", "订单利润缺失", orderWithoutProfitCount, "blocking", "补齐订单利润字段后再进入利润分析"),
                buildQualityCheck("inventory_below_safety", "低库存未修复", lowStockCount, "blocking", "优先清理低库存并回补补货计划"),
                buildQualityCheck("pending_approval", "待审批单据积压", pendingApprovalCount, "warning", "压缩审批积压，避免影响经营编排"),
                buildQualityCheck("notification_dead_letter", "通知死信待处理", deadLetterCount, "warning", "重排死信任务并检查接收人配置"),
                buildQualityCheck("dormant_member", "沉默会员待召回", dormantMemberCount, "warning", "触发会员召回和复购经营任务")
        );
    }

    public BiRepairTaskView repairBiData(String tenantId,
                                         String operatorId,
                                         RepairBiDataRequest request) {
        String checkCode = normalizeRequired(request.checkCode(), "checkCode");
        String repairStrategy = normalizeRequired(request.repairStrategy(), "repairStrategy");
        int affectedCount = currentAffectedCount(tenantId, checkCode);
        if ("notification_dead_letter".equals(checkCode) && affectedCount > 0) {
            List<NotificationTask> deadLetters = notificationTaskRepository.findByTenantId(tenantId).stream()
                    .filter(item -> "dead_letter".equals(item.sendStatus()))
                    .toList();
            for (NotificationTask deadLetter : deadLetters) {
                notificationTaskRepository.save(
                        deadLetter.withDispatchPlan(
                                "planned",
                                OffsetDateTime.now().plusMinutes(10),
                                "bi-repair-" + BI_REPAIR_TASK_SEQUENCE.get()
                        )
                );
            }
        }
        BiRepairTaskState saved = new BiRepairTaskState(
                "bi-repair-" + BI_REPAIR_TASK_SEQUENCE.getAndIncrement(),
                tenantId,
                checkCode,
                repairStrategy,
                "completed",
                affectedCount,
                operatorId,
                normalizeOptionalText(request.remark()),
                OffsetDateTime.now()
        );
        BI_REPAIR_TASK_STORAGE.put(saved.taskId(), saved);
        return toBiRepairTaskView(saved);
    }

    public BiDeliveryChecklistView getBiDeliveryChecklist(String tenantId, String storeId) {
        BiExternalPlatformOverviewView externalBiPlatformOverview = getExternalBiPlatformOverview(tenantId, storeId);
        /*
        List<BiDeliveryCheckItemView> checkItems = List.of(
                new BiDeliveryCheckItemView("theme_domain_ready", "主题域建模", listBiThemeDomains(tenantId, storeId).size() >= 6 ? "completed" : "pending", "订单、商品、供应商、会员、客服、财务主题域已具备最小视图"),
                new BiDeliveryCheckItemView("metric_dictionary_ready", "指标字典", !listBiMetricDefinitions(tenantId).isEmpty() ? "completed" : "pending", "核心经营指标已进入统一口径管理"),
                new BiDeliveryCheckItemView("layer_plan_ready", "分析分层", listBiLayers().size() == 4 ? "completed" : "pending", "ODS/DWD/DWS/ADS 分层方案已就位"),
                new BiDeliveryCheckItemView("analysis_ready", "专题看板", "completed", "经营总览、运营分析、营销分析已可联调"),
                new BiDeliveryCheckItemView("export_ready", "导出与订阅", !listBiExportTasks(tenantId).isEmpty() && !listBiSubscriptions(tenantId).isEmpty() ? "completed" : "pending", "导出任务和日报/周报订阅链路已打通"),
                new BiDeliveryCheckItemView("quality_ready", "数据校验与修复", !BI_REPAIR_TASK_STORAGE.values().stream().filter(item -> tenantId.equals(item.tenantId())).toList().isEmpty() ? "completed" : "pending", "数据质量检查与修复任务机制已具备")
                new BiDeliveryCheckItemView("external_platform_ready", "澶栭儴 BI 骞冲彴", externalBiPlatformOverview.configured() ? "completed" : "pending", externalBiPlatformOverview.configured() ? "Superset 只读工作区与嵌入配置已具备最小接线骨架" : "Superset 只读工作区尚未完成最小配置")
        );
        */
        List<BiDeliveryCheckItemView> checkItems = List.of(
                new BiDeliveryCheckItemView("theme_domain_ready", "theme_domains", listBiThemeDomains(tenantId, storeId).size() >= 6 ? "completed" : "pending", "theme domains are available"),
                new BiDeliveryCheckItemView("metric_dictionary_ready", "metric_dictionary", !listBiMetricDefinitions(tenantId).isEmpty() ? "completed" : "pending", "metric dictionary is available"),
                new BiDeliveryCheckItemView("layer_plan_ready", "layer_plan", listBiLayers().size() == 4 ? "completed" : "pending", "bi layers are available"),
                new BiDeliveryCheckItemView("analysis_ready", "analysis_views", "completed", "overview operations and marketing views are available"),
                new BiDeliveryCheckItemView("export_ready", "export_and_subscription", !listBiExportTasks(tenantId).isEmpty() && !listBiSubscriptions(tenantId).isEmpty() ? "completed" : "pending", "export tasks and subscriptions are available"),
                new BiDeliveryCheckItemView("quality_ready", "data_quality_repair", !BI_REPAIR_TASK_STORAGE.values().stream().filter(item -> tenantId.equals(item.tenantId())).toList().isEmpty() ? "completed" : "pending", "data quality checks and repair tasks are available"),
                new BiDeliveryCheckItemView("external_platform_ready", "external_bi_platform", externalBiPlatformOverview.configured() ? "completed" : "pending", externalBiPlatformOverview.configured() ? "superset read only workspace is ready" : "superset read only workspace is not configured")
        );
        boolean ready = checkItems.stream().allMatch(item -> "completed".equals(item.itemStatus()));
        return new BiDeliveryChecklistView(
                ready ? "ready_for_integration" : "in_progress",
                checkItems,
                OffsetDateTime.now()
        );
    }

    public BiExternalPlatformOverviewView getExternalBiPlatformOverview(String tenantId, String storeId) {
        List<String> linkedThemeDomains = listBiThemeDomains(tenantId, storeId).stream()
                .map(BiThemeDomainView::domainCode)
                .toList();
        String endpoint = environment.getProperty("app.integrations.external.bi.endpoint", "");
        boolean configured = endpoint != null && !endpoint.isBlank();
        String provider = normalizedProperty("app.integrations.external.bi.provider", "superset");
        return new BiExternalPlatformOverviewView(
                provider,
                configured ? "ready" : "fallback_config_required",
                configured,
                resolveHost(endpoint),
                maskEndpoint(endpoint),
                environment.getProperty("app.integrations.external.bi.dashboard-count", Integer.class, linkedThemeDomains.size()),
                environment.getProperty("app.integrations.external.bi.dataset-count", Integer.class, listBiMetricDefinitions(tenantId).size()),
                environment.getProperty("app.integrations.external.bi.embed-enabled", Boolean.class, false),
                linkedThemeDomains
        );
    }

    public void clear() {
        BI_METRIC_DEFINITION_STORAGE.clear();
        BI_EXPORT_TASK_STORAGE.clear();
        BI_SUBSCRIPTION_STORAGE.clear();
        BI_REPAIR_TASK_STORAGE.clear();
        BI_METRIC_DEFINITION_SEQUENCE.set(1L);
        BI_EXPORT_TASK_SEQUENCE.set(1L);
        BI_SUBSCRIPTION_SEQUENCE.set(1L);
        BI_REPAIR_TASK_SEQUENCE.set(1L);
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

    private void requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
    }

    private List<OrderMain> recentOrders(List<String> storeIds, int days) {
        LocalDate threshold = OffsetDateTime.now().toLocalDate().minusDays(days - 1L);
        return orderRepository.findByStoreIds(storeIds).stream()
                .filter(order -> !order.createdAt().toLocalDate().isBefore(threshold))
                .toList();
    }

    private BigDecimal sumOrderAmount(List<OrderMain> orders) {
        return orders.stream()
                .map(OrderMain::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumEstimatedProfit(List<OrderMain> orders) {
        return orders.stream()
                .map(OrderMain::estimatedProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isLowStock(InventorySnapshot snapshot) {
        return snapshot.availableStock() <= snapshot.safetyStock();
    }

    private boolean isDormantMember(OffsetDateTime lastOrderAt) {
        if (lastOrderAt == null) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(lastOrderAt.toLocalDate(), OffsetDateTime.now().toLocalDate());
        return days > 90;
    }

    private boolean isRecentWithinDays(OffsetDateTime time, int days) {
        if (time == null) {
            return false;
        }
        return !time.toLocalDate().isBefore(OffsetDateTime.now().toLocalDate().minusDays(days - 1L));
    }

    private boolean isActivePublishedCampaign(CampaignActivity campaignActivity) {
        OffsetDateTime now = OffsetDateTime.now();
        return "published".equals(campaignActivity.status())
                && !campaignActivity.startAt().isAfter(now)
                && !campaignActivity.endAt().isBefore(now);
    }

    private String freshnessFromOrders(List<OrderMain> orders) {
        return freshnessByLatestOrder(orders.stream()
                .map(OrderMain::createdAt)
                .max(Comparator.naturalOrder())
                .orElse(null));
    }

    private String freshnessFromSnapshots(List<InventorySnapshot> snapshots) {
        return freshnessByLatestOrder(snapshots.stream()
                .map(InventorySnapshot::snapshotAt)
                .max(Comparator.naturalOrder())
                .orElse(null));
    }

    private String freshnessFromSuppliers(List<Supplier> suppliers) {
        return freshnessByLatestOrder(suppliers.stream()
                .map(Supplier::createdAt)
                .max(Comparator.naturalOrder())
                .orElse(null));
    }

    private String freshnessFromMembers(List<MemberProfile> members) {
        return freshnessByLatestOrder(members.stream()
                .map(member -> member.lastOrderAt() == null ? member.createdAt() : member.lastOrderAt())
                .max(Comparator.naturalOrder())
                .orElse(null));
    }

    private String freshnessFromFinanceBills(List<FinanceBill> financeBills) {
        return freshnessByLatestOrder(financeBills.stream()
                .map(FinanceBill::createdAt)
                .max(Comparator.naturalOrder())
                .orElse(null));
    }

    private String freshnessByLatestOrder(OffsetDateTime latestTime) {
        if (latestTime == null) {
            return "empty";
        }
        long days = ChronoUnit.DAYS.between(latestTime.toLocalDate(), OffsetDateTime.now().toLocalDate());
        return days <= 3 ? "fresh" : "stale";
    }

    private String normalizedProperty(String key, String defaultValue) {
        String value = environment.getProperty(key, defaultValue);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String resolveHost(String rawEndpoint) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawEndpoint);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String maskEndpoint(String rawEndpoint) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawEndpoint);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme();
            String host = uri.getHost() == null ? "" : uri.getHost();
            String authority = host;
            if (uri.getPort() >= 0) {
                authority = authority + ":" + uri.getPort();
            }
            if (scheme.isBlank() || authority.isBlank()) {
                return "***";
            }
            return scheme + "://" + authority + "/***";
        } catch (IllegalArgumentException exception) {
            return "***";
        }
    }

    private List<BiMetricDefinitionView> builtinMetricDefinitions(String tenantId) {
        OffsetDateTime now = OffsetDateTime.now();
        return List.of(
                new BiMetricDefinitionView("builtin-sales-income-amount", tenantId, "sales_income_amount", "销售额", "overview", "sum(order.totalAmount)", "最近周期销售额汇总", "order", true, now),
                new BiMetricDefinitionView("builtin-net-profit-amount", tenantId, "net_profit_amount", "净利润", "overview", "sum(order.estimatedProfit)", "最近周期净利润汇总", "finance", true, now),
                new BiMetricDefinitionView("builtin-repurchase-rate", tenantId, "repurchase_rate", "复购率", "overview", "repurchaseMembers / totalMembers", "复购会员数占比", "member", true, now),
                new BiMetricDefinitionView("builtin-fulfillment-pending-count", tenantId, "fulfillment_pending_count", "待确认履约数", "operations", "count(fulfillment.status='pending_confirm')", "待确认履约任务数量", "order", true, now),
                new BiMetricDefinitionView("builtin-high-risk-supplier-count", tenantId, "high_risk_supplier_count", "高风险供应商数", "operations", "count(supplier.riskLevel in ['high','critical'])", "高风险供应商数量", "supplier", true, now),
                new BiMetricDefinitionView("builtin-ticket-satisfaction-score", tenantId, "ticket_satisfaction_score", "客服满意度", "operations", "avg(ticket.satisfactionScore)", "客服满意度平均分", "service", true, now),
                new BiMetricDefinitionView("builtin-marketing-roi-rate", tenantId, "marketing_roi_rate", "营销 ROI", "marketing", "grossProfit / couponCost", "营销毛利对优惠投放成本的回报率", "campaign", true, now),
                new BiMetricDefinitionView("builtin-campaign-conversion-rate", tenantId, "campaign_conversion_rate", "活动转化率", "marketing", "publishedCampaignCount / totalCampaignCount", "已发布活动占全部活动的比例", "campaign", true, now)
        );
    }

    private BiDataQualityCheckView buildQualityCheck(String checkCode,
                                                     String checkName,
                                                     int affectedCount,
                                                     String severity,
                                                     String suggestion) {
        if (affectedCount <= 0) {
            return new BiDataQualityCheckView(checkCode, checkName, "healthy", 0, suggestion);
        }
        return new BiDataQualityCheckView(checkCode, checkName, severity, affectedCount, suggestion);
    }

    private int currentAffectedCount(String tenantId, String checkCode) {
        return listBiDataQualityChecks(tenantId, null).stream()
                .filter(item -> checkCode.equals(item.checkCode()))
                .findFirst()
                .map(BiDataQualityCheckView::affectedCount)
                .orElseThrow(() -> new BusinessException("1003", "data quality check not found", HttpStatus.NOT_FOUND));
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new BusinessException("1002", fieldName + " is required", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BiMetricDefinitionView toBiMetricDefinitionView(BiMetricDefinitionState state) {
        return new BiMetricDefinitionView(
                state.metricId(),
                state.tenantId(),
                state.metricCode(),
                state.metricName(),
                state.metricCategory(),
                state.metricFormula(),
                state.metricDescription(),
                state.ownerDomain(),
                state.enabled(),
                state.updatedAt()
        );
    }

    private BiExportTaskView toBiExportTaskView(BiExportTaskState state) {
        return new BiExportTaskView(
                state.taskId(),
                state.tenantId(),
                state.reportCode(),
                state.storeId(),
                state.exportFormat(),
                state.exportStatus(),
                state.operatorId(),
                state.remark(),
                state.createdAt()
        );
    }

    private BiSubscriptionView toBiSubscriptionView(BiSubscriptionState state) {
        return new BiSubscriptionView(
                state.subscriptionId(),
                state.tenantId(),
                state.subscriptionType(),
                state.reportCode(),
                state.scheduleType(),
                state.channel(),
                state.recipient(),
                state.enabled(),
                state.operatorId(),
                state.remark(),
                state.createdAt()
        );
    }

    private BiRepairTaskView toBiRepairTaskView(BiRepairTaskState state) {
        return new BiRepairTaskView(
                state.taskId(),
                state.tenantId(),
                state.checkCode(),
                state.repairStrategy(),
                state.repairStatus(),
                state.affectedCount(),
                state.operatorId(),
                state.remark(),
                state.createdAt()
        );
    }

    public record DashboardSummaryView(
            BigDecimal todaySalesAmount,
            int todayOrderCount,
            BigDecimal grossProfit,
            int exceptionCount,
            int pendingConfirmCount,
            int lowStockCount,
            String topSuggestion
    ) {
    }

    public record DashboardTrendView(
            LocalDate date,
            BigDecimal salesAmount,
            int orderCount,
            BigDecimal grossProfit
    ) {
    }

    public record DashboardRiskView(
            String riskCode,
            String riskName,
            String riskLevel,
            int riskCount,
            String suggestion
    ) {
    }

    public record DashboardCampaignAnalysisView(
            int totalCampaignCount,
            int publishedCampaignCount,
            int pendingApprovalCount,
            BigDecimal publishSuccessRate,
            int totalCouponTemplateCount,
            String recommendedStrategy
    ) {
    }

    public record DashboardMemberAnalysisView(
            int totalMembers,
            int vipMembers,
            int dormantMembers,
            BigDecimal averagePaidAmount,
            BigDecimal repurchaseRate
    ) {
    }

    public record BiThemeDomainView(
            String domainCode,
            String domainName,
            String dataOwner,
            int objectCount,
            int metricCount,
            String freshnessStatus,
            String layerCode
    ) {
    }

    public record SaveBiMetricDefinitionRequest(
            String metricCode,
            String metricName,
            String metricCategory,
            String metricFormula,
            String metricDescription,
            String ownerDomain,
            Boolean enabled
    ) {
    }

    public record BiMetricDefinitionView(
            String metricId,
            String tenantId,
            String metricCode,
            String metricName,
            String metricCategory,
            String metricFormula,
            String metricDescription,
            String ownerDomain,
            boolean enabled,
            OffsetDateTime updatedAt
    ) {
    }

    public record BiLayerDefinitionView(
            String layerCode,
            String layerName,
            String sourceScope,
            String grainDefinition,
            String outputDefinition,
            String layerStatus
    ) {
    }

    public record BiOverviewView(
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal salesIncomeAmount,
            BigDecimal netProfitAmount,
            BigDecimal repurchaseRate,
            int activeCampaignCount,
            int activeMemberCount,
            String recommendedAction
    ) {
    }

    public record BiOperationalAnalysisView(
            int pendingFulfillmentCount,
            int overdueFulfillmentCount,
            int supplierCount,
            int highRiskSupplierCount,
            int ticketCount,
            int overdueTicketCount,
            BigDecimal averageSatisfactionScore,
            String recommendedAction
    ) {
    }

    public record BiMarketingAnalysisView(
            int totalCampaignCount,
            int publishedCampaignCount,
            BigDecimal marketingRoiRate,
            BigDecimal campaignConversionRate,
            int couponTemplateCount,
            int couponBindCampaignCount,
            String recommendedAction
    ) {
    }

    public record SaveBiExportTaskRequest(
            String reportCode,
            String storeId,
            String exportFormat,
            String remark
    ) {
    }

    public record BiExportTaskView(
            String taskId,
            String tenantId,
            String reportCode,
            String storeId,
            String exportFormat,
            String exportStatus,
            String operatorId,
            String remark,
            OffsetDateTime createdAt
    ) {
    }

    public record BiCockpitView(
            String headline,
            String dailyBrief,
            String weeklyBrief,
            int alertCount,
            List<String> alertItems,
            List<String> recommendedReports
    ) {
    }

    public record SaveBiSubscriptionRequest(
            String subscriptionType,
            String reportCode,
            String scheduleType,
            String channel,
            String recipient,
            Boolean enabled,
            String remark
    ) {
    }

    public record BiSubscriptionView(
            String subscriptionId,
            String tenantId,
            String subscriptionType,
            String reportCode,
            String scheduleType,
            String channel,
            String recipient,
            boolean enabled,
            String operatorId,
            String remark,
            OffsetDateTime createdAt
    ) {
    }

    public record BiDataQualityCheckView(
            String checkCode,
            String checkName,
            String checkStatus,
            int affectedCount,
            String suggestion
    ) {
    }

    public record RepairBiDataRequest(
            String checkCode,
            String repairStrategy,
            String remark
    ) {
    }

    public record BiRepairTaskView(
            String taskId,
            String tenantId,
            String checkCode,
            String repairStrategy,
            String repairStatus,
            int affectedCount,
            String operatorId,
            String remark,
            OffsetDateTime createdAt
    ) {
    }

    public record BiDeliveryChecklistView(
            String overallStatus,
            List<BiDeliveryCheckItemView> checkItems,
            OffsetDateTime generatedAt
    ) {
    }

    public record BiExternalPlatformOverviewView(
            String provider,
            String overviewStatus,
            boolean configured,
            String host,
            String maskedEndpoint,
            int dashboardCount,
            int datasetCount,
            boolean embedEnabled,
            List<String> linkedThemeDomains
    ) {
    }

    public record BiDeliveryCheckItemView(
            String itemCode,
            String itemName,
            String itemStatus,
            String detail
    ) {
    }

    private record BiMetricDefinitionState(
            String metricId,
            String tenantId,
            String metricCode,
            String metricName,
            String metricCategory,
            String metricFormula,
            String metricDescription,
            String ownerDomain,
            boolean enabled,
            OffsetDateTime updatedAt
    ) {
    }

    private record BiExportTaskState(
            String taskId,
            String tenantId,
            String reportCode,
            String storeId,
            String exportFormat,
            String exportStatus,
            String operatorId,
            String remark,
            OffsetDateTime createdAt
    ) {
    }

    private record BiSubscriptionState(
            String subscriptionId,
            String tenantId,
            String subscriptionType,
            String reportCode,
            String scheduleType,
            String channel,
            String recipient,
            boolean enabled,
            String operatorId,
            String remark,
            OffsetDateTime createdAt
    ) {
    }

    private record BiRepairTaskState(
            String taskId,
            String tenantId,
            String checkCode,
            String repairStrategy,
            String repairStatus,
            int affectedCount,
            String operatorId,
            String remark,
            OffsetDateTime createdAt
    ) {
    }
}
