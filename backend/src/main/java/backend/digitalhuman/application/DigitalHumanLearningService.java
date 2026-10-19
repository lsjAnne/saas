package backend.digitalhuman.application;

import backend.ai.VectorFeature;
import backend.common.exception.BusinessException;
import backend.digitalhuman.domain.DigitalHumanQuestionTypes;
import backend.digitalhuman.domain.repository.DigitalHumanFaqRepository;
import backend.digitalhuman.domain.repository.DigitalHumanLearningItemRepository;
import backend.digitalhuman.model.DigitalHumanFaq;
import backend.digitalhuman.model.DigitalHumanLearningItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数字人学习记忆服务：自成长闭环的核心。
 *
 * 记忆：每条"未命中知识库"的观众问题都会写入学习记忆（不丢失任何提问）；
 * 聚类：新问题与历史未答问题做向量语义匹配（VectorFeature 余弦相似度），同义改写自动归并为同一条，次数累积；
 * 提炼：同类问题出现次数达到阈值（3 次）自动生成 FAQ 入库——同商品同类型的既有答案直接复用，否则生成待完善条目，知识库随直播运营持续自我补充；
 * 成长：提供成长指标（知识库增长、自动学习转化、未答回收、高频待答问题），数字人的"经验"可视化。
 *
 * 语义聚类使用内置 Java 向量引擎（零外部依赖）；生产可切换 pgvector 存储，不影响本服务逻辑。
 */
@Service
public class DigitalHumanLearningService {

    /** 语义归并阈值：与历史未答问题余弦相似度达到该值视为同一个问题（问题类型一致前提下） */
    public static final float MERGE_THRESHOLD = 0.55f;

    /** 自动提炼阈值：同类问题出现次数达到该值自动入库 */
    public static final long AUTO_LEARN_THRESHOLD = 3;

    public static final String STATUS_NEW = "new";
    public static final String STATUS_LEARNING = "learning";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_IGNORED = "ignored";

    /** 自动入库但暂无标准答案时的待完善话术（运营可在知识库中补充答案） */
    public static final String TEMPLATE_ANSWER = "亲，这个问题大家都很关心，主播正在为您整理详细答复，稍后更新～";

    private final DigitalHumanLearningItemRepository itemRepository;
    private final DigitalHumanFaqRepository faqRepository;

    public DigitalHumanLearningService(DigitalHumanLearningItemRepository itemRepository,
                                       DigitalHumanFaqRepository faqRepository) {
        this.itemRepository = itemRepository;
        this.faqRepository = faqRepository;
    }

    /**
     * 记忆一条未命中知识库的问题。
     * 语义匹配到历史同类问题 → 次数 +1；达到阈值自动提炼入知识库并转 confirmed；
     * 无匹配 → 新建学习条目（status=new）。
     *
     * @return 更新后的学习条目（可能为新建条目）
     */
    public DigitalHumanLearningItem recordUnanswered(String tenantId, String organizationId,
                                                     String sessionId, String question, String productName) {
        String questionType = DigitalHumanQuestionTypes.detect(question);
        DigitalHumanLearningItem existing = findMergeable(tenantId, question, questionType);
        if (existing != null) {
            long count = existing.occurrenceCount() + 1;
            DigitalHumanLearningItem updated = existing.withOccurrence(count, OffsetDateTime.now());
            // 已入库（confirmed）的问题：只累计热度，保持已确认状态，不再重复提炼
            if (STATUS_CONFIRMED.equals(existing.status())) {
                return itemRepository.save(updated);
            }
            if (count >= AUTO_LEARN_THRESHOLD) {
                return itemRepository.save(autoLearn(updated));
            }
            if (count == 2) {
                return itemRepository.save(updated.withStatus(STATUS_LEARNING));
            }
            return itemRepository.save(updated);
        }
        DigitalHumanLearningItem created = new DigitalHumanLearningItem(
                null, tenantId, organizationId, sessionId, question, questionType,
                productName, 1L, STATUS_NEW, null, OffsetDateTime.now(), OffsetDateTime.now()
        );
        return itemRepository.save(created);
    }

    /** 运营确认：将该条未答问题立即转正入知识库 */
    public DigitalHumanLearningItem confirm(String tenantId, String itemId) {
        DigitalHumanLearningItem item = requireItem(tenantId, itemId);
        if (STATUS_IGNORED.equals(item.status())) {
            return item;
        }
        return itemRepository.save(autoLearn(item));
    }

    /** 运营忽略：该问题不再参与自动提炼 */
    public DigitalHumanLearningItem ignore(String tenantId, String itemId) {
        DigitalHumanLearningItem item = requireItem(tenantId, itemId);
        return itemRepository.save(item.withStatus(STATUS_IGNORED));
    }

    /** 学习记忆列表（按最近出现时间倒序） */
    public List<DigitalHumanLearningItem> listItems(String tenantId) {
        return itemRepository.listByTenant(tenantId);
    }

    /** 成长指标：数字人"越用越懂"的可视化数据 */
    public Map<String, Object> growth(String tenantId) {
        List<DigitalHumanLearningItem> items = itemRepository.listByTenant(tenantId);
        List<DigitalHumanFaq> faqs = faqRepository.listByTenant(tenantId);

        long newCount = items.stream().filter(i -> STATUS_NEW.equals(i.status())).count();
        long learningCount = items.stream().filter(i -> STATUS_LEARNING.equals(i.status())).count();
        long confirmedCount = items.stream().filter(i -> STATUS_CONFIRMED.equals(i.status())).count();
        long ignoredCount = items.stream().filter(i -> STATUS_IGNORED.equals(i.status())).count();

        // 自动学习入库数：confirmed 条目关联的 FAQ 仍存在（去重）；其中未启用的为"待完善"（运营补答后启用）
        List<String> learnedFaqIds = items.stream()
                .filter(i -> STATUS_CONFIRMED.equals(i.status()))
                .map(DigitalHumanLearningItem::faqId)
                .filter(faqId -> faqId != null && !faqId.isBlank())
                .distinct()
                .toList();
        long autoLearnedCount = learnedFaqIds.stream()
                .filter(faqId -> faqs.stream().anyMatch(f -> faqId.equals(f.faqId())))
                .count();
        long pendingFaqCount = learnedFaqIds.stream()
                .filter(faqId -> faqs.stream().anyMatch(f -> faqId.equals(f.faqId()) && !f.enabled()))
                .count();

        long enabledFaqCount = faqs.stream().filter(DigitalHumanFaq::enabled).count();
        long manualFaqCount = Math.max(0, enabledFaqCount - (autoLearnedCount - pendingFaqCount));

        // 记忆转化率：已被知识库吸收的未答问题占比
        long totalTracked = newCount + learningCount + confirmedCount + ignoredCount;
        double conversionRate = totalTracked == 0 ? 0d
                : Math.round((confirmedCount * 1000d) / totalTracked) / 10d;

        // 高频待答问题（new/learning 按次数倒序 top 5，运营优先补答）
        List<Map<String, Object>> hotUnanswered = items.stream()
                .filter(i -> STATUS_NEW.equals(i.status()) || STATUS_LEARNING.equals(i.status()))
                .sorted(Comparator.comparingLong(DigitalHumanLearningItem::occurrenceCount).reversed())
                .limit(5)
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("itemId", i.itemId());
                    m.put("question", i.question());
                    m.put("questionType", i.questionType());
                    m.put("productName", i.productName());
                    m.put("occurrenceCount", i.occurrenceCount());
                    m.put("status", i.status());
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("faqCount", enabledFaqCount);
        result.put("manualFaqCount", manualFaqCount);
        result.put("autoLearnedCount", autoLearnedCount);
        result.put("pendingFaqCount", pendingFaqCount);
        result.put("memoryTotal", totalTracked);
        result.put("statusNewCount", newCount);
        result.put("statusLearningCount", learningCount);
        result.put("statusConfirmedCount", confirmedCount);
        result.put("statusIgnoredCount", ignoredCount);
        result.put("conversionRate", conversionRate);
        result.put("hotUnanswered", hotUnanswered);
        return result;
    }

    /**
     * 自动提炼：把学习条目转正为 FAQ 条目。
     * 答案来源优先级：① 同商品（含均未识别商品）同类型的既有启用 FAQ 答案（同类问题答案通常一致）→ ② 无答案源。
     * 有答案源 → 生成的 FAQ 直接启用（数字人立即可答）；
     * 无答案源 → 生成的 FAQ 标记为待完善（enabled=false），保留在知识库列表供运营补答后启用，避免答非所问。
     * 若该条目已关联 FAQ 则直接确认（避免重复建库）。
     */
    private DigitalHumanLearningItem autoLearn(DigitalHumanLearningItem item) {
        if (item.faqId() != null && !item.faqId().isBlank()) {
            return item.withStatus(STATUS_CONFIRMED);
        }
        DigitalHumanFaq source = findSourceFaq(item.tenantId(), item.productName(), item.questionType());
        boolean enabled = source != null;
        String answer = enabled ? source.answer() : TEMPLATE_ANSWER;
        DigitalHumanFaq faq = new DigitalHumanFaq(
                null,
                item.tenantId(),
                item.organizationId(),
                null,
                item.productName(),
                item.questionType(),
                "",
                item.question(),
                answer,
                enabled,
                0L,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        DigitalHumanFaq saved = faqRepository.save(faq);
        return item.withFaq(saved.faqId()).withStatus(STATUS_CONFIRMED);
    }

    /** 语义匹配：同类型、未忽略的学习条目中与 question 最相近的一条 */
    private DigitalHumanLearningItem findMergeable(String tenantId, String question, String questionType) {
        float[] queryVector = VectorFeature.encode(question);
        DigitalHumanLearningItem best = null;
        float bestScore = MERGE_THRESHOLD;
        for (DigitalHumanLearningItem item : itemRepository.listByTenant(tenantId)) {
            if (STATUS_IGNORED.equals(item.status())) {
                continue;
            }
            if (!questionType.equals(item.questionType())) {
                continue;
            }
            float score = VectorFeature.cosine(queryVector, VectorFeature.encode(item.question()));
            if (score >= bestScore) {
                bestScore = score;
                best = item;
            }
        }
        return best;
    }

    /** 答案来源：同商品（含均未识别商品）同类型的首个启用 FAQ */
    private DigitalHumanFaq findSourceFaq(String tenantId, String productName, String questionType) {
        for (DigitalHumanFaq faq : faqRepository.listByTenant(tenantId)) {
            if (!faq.enabled()) {
                continue;
            }
            boolean sameProduct = productName == null ? faq.productName() == null : productName.equals(faq.productName());
            if (sameProduct && questionType.equals(faq.questionType())) {
                return faq;
            }
        }
        return null;
    }

    private DigitalHumanLearningItem requireItem(String tenantId, String itemId) {
        return itemRepository.findById(tenantId, itemId)
                .orElseThrow(() -> new BusinessException("2201", "learning item not found: " + itemId, HttpStatus.NOT_FOUND));
    }
}
