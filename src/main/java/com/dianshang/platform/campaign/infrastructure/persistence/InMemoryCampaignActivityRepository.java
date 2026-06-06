package com.dianshang.platform.campaign.infrastructure.persistence;

import com.dianshang.platform.campaign.domain.repository.CampaignActivityRepository;
import com.dianshang.platform.campaign.model.CampaignActivity;
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
public class InMemoryCampaignActivityRepository implements CampaignActivityRepository {

    private final Map<String, CampaignActivity> storage = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public List<CampaignActivity> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(campaign -> storeIds.contains(campaign.storeId()))
                .sorted(Comparator.comparing(CampaignActivity::createdAt).reversed()
                        .thenComparing(CampaignActivity::campaignId, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public Optional<CampaignActivity> findByCampaignId(String campaignId) {
        return Optional.ofNullable(storage.get(campaignId));
    }

    @Override
    public CampaignActivity save(CampaignActivity campaignActivity) {
        CampaignActivity stored = campaignActivity;
        if (campaignActivity.campaignId() == null || campaignActivity.campaignId().isBlank()) {
            stored = new CampaignActivity(
                    "campaign-" + sequence.getAndIncrement(),
                    campaignActivity.storeId(),
                    campaignActivity.activityType(),
                    campaignActivity.activityName(),
                    campaignActivity.status(),
                    campaignActivity.startAt(),
                    campaignActivity.endAt(),
                    campaignActivity.productIds(),
                    campaignActivity.rule(),
                    campaignActivity.couponTemplateId(),
                    campaignActivity.needApproval(),
                    campaignActivity.createdAt()
            );
        }
        storage.put(stored.campaignId(), stored);
        return stored;
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(1L);
    }
}
