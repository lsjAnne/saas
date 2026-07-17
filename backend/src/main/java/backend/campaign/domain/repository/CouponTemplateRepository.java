package backend.campaign.domain.repository;

import backend.campaign.model.CouponTemplate;

import java.util.List;
import java.util.Optional;

public interface CouponTemplateRepository {

    List<CouponTemplate> findByStoreIds(List<String> storeIds);

    Optional<CouponTemplate> findByCouponTemplateId(String couponTemplateId);

    CouponTemplate save(CouponTemplate couponTemplate);

    void deleteAll();
}

