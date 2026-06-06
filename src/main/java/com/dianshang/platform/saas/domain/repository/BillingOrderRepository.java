package com.dianshang.platform.saas.domain.repository;

import com.dianshang.platform.saas.model.BillingOrder;

import java.util.List;
import java.util.Optional;

public interface BillingOrderRepository {

    BillingOrder save(BillingOrder billingOrder);

    List<BillingOrder> findByTenantId(String tenantId);

    Optional<BillingOrder> findByBillingOrderId(String billingOrderId);

    void deleteAll();
}
