package backend.digitalhuman.application;

import backend.common.exception.BusinessException;
import backend.ai.FaqSemanticSearch;
import backend.digitalhuman.domain.DigitalHumanQuestionTypes;
import backend.digitalhuman.domain.repository.DigitalHumanBarrageRepository;
import backend.digitalhuman.domain.repository.DigitalHumanFaqRepository;
import backend.digitalhuman.domain.repository.DigitalHumanInteractionRepository;
import backend.digitalhuman.model.DigitalHumanBarrage;
import backend.digitalhuman.model.DigitalHumanFaq;
import backend.digitalhuman.model.DigitalHumanInteraction;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数字人弹幕问答服务：模拟真人主播"看弹幕→识别问题→按商品与问题类型作答"。
 *
 * 混合问答路由：
 *  ① 语义检索（向量）：对弹幕做特征向量化，与问答知识库做余弦相似度检索，同义改写（"这个耳机多少钱"/"价格多少"）也能命中；
 *  ② 关键词路由：按商品与问题关键词精确匹配，覆盖语义检索未达阈值的问法；
 *  ③ 未命中：使用兜底话术，并把问题写入学习记忆（自成长闭环——同类问题达到阈值自动沉淀进知识库）。
 * TODO(联调)：真实模式下弹幕由平台开放平台评论流接入；开放性问题可接大模型生成回答（hitType=llm）。
 */
@Service
public class DigitalHumanInteractionService {

    /** 语义命中阈值：向量相似度达到该值认为弹幕与该知识条目是同一问题 */
    private static final float SEMANTIC_THRESHOLD = 0.45f;

    /** 未命中兜底话术（模拟真人主播的转圜表达） */
    private static final List<String> FALLBACK_ANSWERS = List.of(
            "这个问题主播先记下来啦，稍后让运营同事专门答复您～",
            "刚看到您的弹幕，这个细节我马上帮您确认一下，稍等片刻～",
            "亲亲稍等，这个问题我请专业客服跟进，马上回复您～",
            "感谢提问～这个信息我核实后再给您准确答复，先别走开哦"
    );

    /** 模拟弹幕池（覆盖多商品、多问题类型与兜底场景，用于演示与测试） */
    private static final List<String> SIMULATED_BARRAGES = List.of(
            "这个蓝牙耳机多少钱呀",
            "无线耳机续航怎么样",
            "蓝牙耳机延迟高不高",
            "羽绒服有尺码推荐吗",
            "羽绒服保暖吗 会跑毛吗",
            "羽绒服多久发货",
            "羽绒服可以退货吗",
            "有优惠券吗",
            "包邮吗",
            "怎么下单啊",
            "主播在吗 有链接吗",
            "这个质量怎么样"
    );

    private final DigitalHumanFaqRepository faqRepository;
    private final DigitalHumanBarrageRepository barrageRepository;
    private final DigitalHumanInteractionRepository interactionRepository;
    private final DigitalHumanLearningService learningService;
    private final FaqSemanticSearch semanticSearch;

    public DigitalHumanInteractionService(DigitalHumanFaqRepository faqRepository,
                                          DigitalHumanBarrageRepository barrageRepository,
                                          DigitalHumanInteractionRepository interactionRepository,
                                          DigitalHumanLearningService learningService,
                                          FaqSemanticSearch semanticSearch) {
        this.faqRepository = faqRepository;
        this.barrageRepository = barrageRepository;
        this.interactionRepository = interactionRepository;
        this.learningService = learningService;
        this.semanticSearch = semanticSearch;
    }

    // ---------- 问答知识库 ----------

    public DigitalHumanFaq createFaq(String tenantId, CreateFaqRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        return faqRepository.save(new DigitalHumanFaq(
                null,
                tenantId,
                request.organizationId(),
                request.productId(),
                request.productName(),
                request.questionType() == null || request.questionType().isBlank() ? DigitalHumanQuestionTypes.GENERIC : request.questionType(),
                request.keywords(),
                request.question(),
                request.answer(),
                request.enabled() == null || request.enabled(),
                0,
                now,
                now
        ));
    }

    public DigitalHumanFaq updateFaq(String tenantId, String faqId, CreateFaqRequest request) {
        DigitalHumanFaq existing = requireFaq(tenantId, faqId);
        return faqRepository.save(existing.withEditableFields(
                request.productId(),
                request.productName(),
                request.questionType() == null || request.questionType().isBlank() ? DigitalHumanQuestionTypes.GENERIC : request.questionType(),
                request.keywords(),
                request.question(),
                request.answer(),
                request.enabled() == null || request.enabled()
        ));
    }

    public List<DigitalHumanFaq> listFaqs(String tenantId) {
        return faqRepository.listByTenant(tenantId);
    }

    public void deleteFaq(String tenantId, String faqId) {
        requireFaq(tenantId, faqId);
        faqRepository.deleteById(tenantId, faqId);
    }

    // ---------- 弹幕问答 ----------

    /**
     * 接收一条观众弹幕并立即生成回答：存弹幕 → 意图路由 → 写互动记录。
     * 返回弹幕与回答，供前端直播间聊天区实时上屏。
     */
    public InteractionReplyView submitBarrage(String tenantId, SubmitBarrageRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        DigitalHumanBarrage saved = barrageRepository.save(new DigitalHumanBarrage(
                null,
                tenantId,
                request.organizationId(),
                request.sessionId(),
                request.platformType() == null || request.platformType().isBlank() ? "mock" : request.platformType(),
                request.userNick() == null || request.userNick().isBlank() ? "观众" + ThreadLocalRandom.current().nextInt(1000, 9999) : request.userNick(),
                request.content(),
                "pending",
                now
        ));

        AnswerHit hit = answerFor(tenantId, request.sessionId(), saved.content());
        if ("fallback".equals(hit.hitType())) {
            // 未命中 → 写入学习记忆（自成长）：识别弹幕商品名，交给学习服务做语义聚类与自动提炼
            String productName = detectProduct(
                    faqRepository.listByTenant(tenantId).stream().filter(DigitalHumanFaq::enabled).toList(),
                    saved.content()
            ).map(DigitalHumanFaq::productName).orElse(null);
            learningService.recordUnanswered(tenantId, request.organizationId(), request.sessionId(), saved.content(), productName);
        }
        DigitalHumanBarrage replied = barrageRepository.save(saved.withReplyStatus(hit.hitType().equals("faq") ? "replied" : "skipped"));
        DigitalHumanInteraction interaction = interactionRepository.save(new DigitalHumanInteraction(
                null,
                tenantId,
                request.organizationId(),
                request.sessionId(),
                replied.barrageId(),
                hit.hitType(),
                hit.faqId(),
                saved.content(),
                hit.answer(),
                now
        ));
        return new InteractionReplyView(replied, interaction);
    }

    /**
     * 模拟一批观众弹幕（演示用）：从预置弹幕池随机抽取 count 条并逐条作答。
     */
    public List<InteractionReplyView> simulateBarrages(String tenantId, SimulateBarrageRequest request) {
        int count = request.count() == null ? 6 : Math.min(Math.max(request.count(), 1), 12);
        List<String> pool = new ArrayList<>(SIMULATED_BARRAGES);
        List<InteractionReplyView> replies = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String content = pool.remove(ThreadLocalRandom.current().nextInt(pool.size()));
            SubmitBarrageRequest one = new SubmitBarrageRequest(
                    request.organizationId(),
                    request.sessionId(),
                    request.platformType(),
                    "观众" + ThreadLocalRandom.current().nextInt(1000, 9999),
                    content
            );
            replies.add(submitBarrage(tenantId, one));
        }
        return replies;
    }

    public List<DigitalHumanBarrage> listBarrages(String tenantId, String sessionId) {
        return barrageRepository.listBySession(tenantId, sessionId);
    }

    public List<DigitalHumanInteraction> listInteractions(String tenantId, String sessionId) {
        return interactionRepository.listBySession(tenantId, sessionId);
    }

    /**
     * 最近互动（按时间倒序）：弹幕源接入后，前端轮询本方法把最新问答实时上屏。
     */
    public List<DigitalHumanInteraction> listRecentInteractions(String tenantId, int limit) {
        int capped = Math.min(Math.max(limit, 1), 50);
        return interactionRepository.listByTenant(tenantId).stream()
                .sorted(java.util.Comparator.comparing(DigitalHumanInteraction::createdAt).reversed())
                .limit(capped)
                .toList();
    }

    public InteractionOverviewView interactionOverview(String tenantId) {
        List<DigitalHumanFaq> faqs = faqRepository.listByTenant(tenantId);
        List<DigitalHumanBarrage> barrages = barrageRepository.listByTenant(tenantId);
        List<DigitalHumanInteraction> interactions = interactionRepository.listByTenant(tenantId);
        long faqCount = faqs.stream().filter(DigitalHumanFaq::enabled).count();
        long repliedCount = interactions.stream().filter(i -> "faq".equals(i.hitType())).count();
        long fallbackCount = interactions.stream().filter(i -> "fallback".equals(i.hitType())).count();
        BigDecimal hitRate = interactions.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(repliedCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(interactions.size()), 1, RoundingMode.HALF_UP);
        List<HotQuestionView> hotQuestions = faqs.stream()
                .filter(DigitalHumanFaq::enabled)
                .sorted((a, b) -> Long.compare(b.hitCount(), a.hitCount()))
                .limit(5)
                .map(faq -> new HotQuestionView(faq.productName(), faq.question(), faq.hitCount()))
                .toList();
        return new InteractionOverviewView(
                faqCount,
                barrages.size(),
                repliedCount,
                fallbackCount,
                hitRate,
                hotQuestions
        );
    }

    // ---------- 意图路由核心 ----------

    private record AnswerHit(String hitType, String faqId, String answer) {
    }

    /**
     * 针对一条弹幕生成回答（混合路由）：
     * ① 语义检索：向量相似度命中同商品（或未识别商品时的通用条目）的 FAQ，优先采用；
     * ② 商品识别 + 关键词/文本路由：按商品名与问题类型精确匹配；
     * ③ 未命中：使用兜底话术（同时进入学习记忆，见 submitBarrage）。
     */
    private AnswerHit answerFor(String tenantId, String sessionId, String content) {
        List<DigitalHumanFaq> faqs = faqRepository.listByTenant(tenantId).stream()
                .filter(DigitalHumanFaq::enabled)
                .toList();
        if (faqs.isEmpty()) {
            return new AnswerHit("fallback", null, pickFallback());
        }

        Optional<DigitalHumanFaq> productFaq = detectProduct(faqs, content);
        String productName = productFaq.map(DigitalHumanFaq::productName).orElse(null);
        String questionType = DigitalHumanQuestionTypes.detect(content);
        DigitalHumanFaq hit = null;

        // ① 语义检索（向量）：同商品命中优先；未识别商品时仅接受通用条目，避免跨商品串台
        if (semanticSearch != null) {
            hit = findSemanticHit(tenantId, faqs, content, productName);
        }

        // ② 关键词/文本路由（语义未命中时）
        if (hit == null && productFaq.isPresent()) {
            List<DigitalHumanFaq> productFaqs = faqs.stream()
                    .filter(f -> productName.equals(f.productName()))
                    .toList();
            hit = productFaqs.stream()
                    .filter(f -> matchesText(f, content))
                    .findFirst()
                    .orElse(null);
            if (hit == null) {
                hit = productFaqs.stream()
                        .filter(f -> questionType.equals(f.questionType()))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (hit == null) {
            hit = faqs.stream()
                    .filter(f -> matchesText(f, content))
                    .findFirst()
                    .orElse(null);
            if (hit == null) {
                hit = faqs.stream()
                        .filter(f -> f.productName() == null || f.productName().isBlank())
                        .filter(f -> questionType.equals(f.questionType()))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (hit != null) {
            DigitalHumanFaq updated = faqRepository.save(hit.withHitCount(hit.hitCount() + 1));
            return new AnswerHit("faq", updated.faqId(), updated.answer());
        }
        return new AnswerHit("fallback", null, pickFallback());
    }

    /**
     * 语义检索候选：按相似度降序取 top 5，要求与商品上下文一致——
     * 弹幕已识别商品 → 仅接受同商品的条目；未识别商品 → 仅接受通用条目（未绑定商品）。
     */
    private DigitalHumanFaq findSemanticHit(String tenantId, List<DigitalHumanFaq> faqs, String content, String productName) {
        for (FaqSemanticSearch.SemanticHit candidate : semanticSearch.searchSimilar(tenantId, content, 5)) {
            if (candidate.score() < SEMANTIC_THRESHOLD) {
                break;
            }
            DigitalHumanFaq faq = faqs.stream()
                    .filter(f -> f.faqId().equals(candidate.faqId()))
                    .findFirst()
                    .orElse(null);
            if (faq == null) {
                continue;
            }
            boolean sameProduct = productName != null
                    ? productName.equals(faq.productName())
                    : (faq.productName() == null || faq.productName().isBlank());
            if (sameProduct) {
                return faq;
            }
        }
        return null;
    }

    /** 识别弹幕提到的商品：匹配启用 FAQ 中商品名，取名字更长（更具体）的候选 */
    private Optional<DigitalHumanFaq> detectProduct(List<DigitalHumanFaq> faqs, String content) {
        return faqs.stream()
                .filter(f -> f.productName() != null && !f.productName().isBlank())
                .filter(f -> content.contains(f.productName()))
                .max((a, b) -> Integer.compare(a.productName().length(), b.productName().length()));
    }

    /** 弹幕文本是否命中 FAQ 的关键词或问题描述 */
    private boolean matchesText(DigitalHumanFaq faq, String content) {
        if (faq.keywords() != null && !faq.keywords().isBlank()) {
            for (String keyword : faq.keywords().split("[,，]")) {
                if (!keyword.isBlank() && content.contains(keyword.trim())) {
                    return true;
                }
            }
        }
        if (faq.question() != null && !faq.question().isBlank() && content.contains(faq.question())) {
            return true;
        }
        return false;
    }

    private String pickFallback() {
        return FALLBACK_ANSWERS.get(ThreadLocalRandom.current().nextInt(FALLBACK_ANSWERS.size()));
    }

    private DigitalHumanFaq requireFaq(String tenantId, String faqId) {
        return faqRepository.findById(tenantId, faqId)
                .orElseThrow(() -> new BusinessException("2101", "faq not found: " + faqId, HttpStatus.NOT_FOUND));
    }

    // ---------- 请求与视图 ----------

    public record CreateFaqRequest(
            @NotBlank String organizationId,
            String productId,
            String productName,
            String questionType,
            String keywords,
            @NotBlank String question,
            @NotBlank String answer,
            Boolean enabled
    ) {
    }

    public record SubmitBarrageRequest(
            @NotBlank String organizationId,
            String sessionId,
            String platformType,
            String userNick,
            @NotBlank String content
    ) {
    }

    public record SimulateBarrageRequest(
            @NotBlank String organizationId,
            String sessionId,
            String platformType,
            Integer count
    ) {
    }

    public record InteractionReplyView(
            DigitalHumanBarrage barrage,
            DigitalHumanInteraction interaction
    ) {
    }

    public record HotQuestionView(
            String productName,
            String question,
            long hitCount
    ) {
    }

    public record InteractionOverviewView(
            long faqCount,
            long barrageCount,
            long repliedCount,
            long fallbackCount,
            BigDecimal hitRate,
            List<HotQuestionView> hotQuestions
    ) {
    }
}
