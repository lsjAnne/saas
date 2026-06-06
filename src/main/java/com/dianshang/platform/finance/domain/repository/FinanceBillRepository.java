package com.dianshang.platform.finance.domain.repository;

import com.dianshang.platform.finance.model.FinanceBill;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinanceBillRepository {

    List<FinanceBill> findByStoreIds(List<String> storeIds);

    Optional<FinanceBill> findByFinanceBillId(String financeBillId);

    Optional<FinanceBill> findByStoreAndPeriod(String storeId, String billType, LocalDate periodStart, LocalDate periodEnd);

    FinanceBill save(FinanceBill financeBill);

    void deleteAll();
}
