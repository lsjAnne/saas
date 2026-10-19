package backend.ai;

import backend.digitalhuman.domain.repository.DigitalHumanFaqRepository;
import backend.digitalhuman.model.DigitalHumanFaq;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内置 Java 向量引擎实现：从 FAQ 仓储实时取数、特征向量化、余弦检索。
 * 零外部依赖，H2/达梦/开发与演示环境均可运行；FAQ 规模在万级以内性能完全足够。
 * 特征缓存按 FAQ id + 文本指纹维护，FAQ 更新后自动失效重建。
 */
@Component
@ConditionalOnProperty(name = "app.ai.vector.store", havingValue = "memory", matchIfMissing = true)
public class MemoryFaqSemanticSearch implements FaqSemanticSearch {

    private final DigitalHumanFaqRepository faqRepository;
    private final ConcurrentHashMap<String, float[]> featureCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> fingerprintCache = new ConcurrentHashMap<>();

    public MemoryFaqSemanticSearch(DigitalHumanFaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Override
    public List<SemanticHit> searchSimilar(String tenantId, String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        float[] queryVector = VectorFeature.encode(query);
        List<SemanticHit> hits = new ArrayList<>();

        for (DigitalHumanFaq faq : faqRepository.listByTenant(tenantId)) {
            if (!faq.enabled()) {
                continue;
            }
            float[] faqVector = cachedFeature(faq);
            if (faqVector == null) {
                continue;
            }
            float score = VectorFeature.cosine(queryVector, faqVector);
            if (score > 0.30f) {
                hits.add(new SemanticHit(faq.faqId(), score));
            }
        }
        hits.sort(Comparator.comparingDouble(SemanticHit::score).reversed());
        return hits.size() > topK ? hits.subList(0, topK) : hits;
    }

    private float[] cachedFeature(DigitalHumanFaq faq) {
        String text = faq.question();
        String fingerprint = VectorFeature.fingerprint(text);
        String cachedFingerprint = fingerprintCache.get(faq.faqId());
        if (cachedFingerprint != null && cachedFingerprint.equals(fingerprint)) {
            float[] cached = featureCache.get(faq.faqId());
            if (cached != null) {
                return cached;
            }
        }
        float[] vector = VectorFeature.encode(text);
        fingerprintCache.put(faq.faqId(), fingerprint);
        featureCache.put(faq.faqId(), vector);
        return vector;
    }
}
