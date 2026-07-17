package backend.campaign.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record CampaignActivity(
        String campaignId,
        String storeId,
        String activityType,
        String activityName,
        String status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        List<String> productIds,
        Map<String, Object> rule,
        String couponTemplateId,
        boolean needApproval,
        OffsetDateTime createdAt
) {
    public CampaignActivity withEditableFields(String activityType,
                                               String activityName,
                                               OffsetDateTime startAt,
                                               OffsetDateTime endAt,
                                               List<String> productIds,
                                               Map<String, Object> rule,
                                               String couponTemplateId,
                                               boolean needApproval) {
        return new CampaignActivity(
                campaignId,
                storeId,
                activityType,
                activityName,
                status,
                startAt,
                endAt,
                productIds,
                rule,
                couponTemplateId,
                needApproval,
                createdAt
        );
    }

    public CampaignActivity withStatus(String status) {
        return new CampaignActivity(
                campaignId,
                storeId,
                activityType,
                activityName,
                status,
                startAt,
                endAt,
                productIds,
                rule,
                couponTemplateId,
                needApproval,
                createdAt
        );
    }
}

