package com.dianshang.platform.campaign.domain.repository;

import com.dianshang.platform.campaign.model.CampaignActivity;

import java.util.List;
import java.util.Optional;

public interface CampaignActivityRepository {

    List<CampaignActivity> findByStoreIds(List<String> storeIds);

    Optional<CampaignActivity> findByCampaignId(String campaignId);

    CampaignActivity save(CampaignActivity campaignActivity);

    void deleteAll();
}
