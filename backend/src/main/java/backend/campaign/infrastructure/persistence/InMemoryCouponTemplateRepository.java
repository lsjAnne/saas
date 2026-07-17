package backend.campaign.infrastructure.persistence;

import backend.campaign.domain.repository.CouponTemplateRepository;
import backend.campaign.model.CouponTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryCouponTemplateRepository implements CouponTemplateRepository {

    private final Map<String, CouponTemplate> storage = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public List<CouponTemplate> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(template -> storeIds.contains(template.storeId()))
                .sorted(Comparator.comparing(CouponTemplate::createdAt).reversed()
                        .thenComparing(CouponTemplate::couponTemplateId, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public Optional<CouponTemplate> findByCouponTemplateId(String couponTemplateId) {
        return Optional.ofNullable(storage.get(couponTemplateId));
    }

    @Override
    public CouponTemplate save(CouponTemplate couponTemplate) {
        CouponTemplate stored = couponTemplate;
        if (couponTemplate.couponTemplateId() == null || couponTemplate.couponTemplateId().isBlank()) {
            stored = new CouponTemplate(
                    "coupon-template-" + sequence.getAndIncrement(),
                    couponTemplate.storeId(),
                    couponTemplate.templateName(),
                    couponTemplate.discountType(),
                    couponTemplate.discountValue(),
                    couponTemplate.thresholdAmount(),
                    couponTemplate.status(),
                    couponTemplate.createdAt()
            );
        }
        storage.put(stored.couponTemplateId(), stored);
        return stored;
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(1L);
    }
}

