package com.dianshang.platform.finance.domain.repository;

import com.dianshang.platform.finance.model.SettlementRecord;

import java.util.List;

public interface SettlementRecordRepository {

    List<SettlementRecord> findByFinanceBillId(String financeBillId);

    SettlementRecord save(SettlementRecord settlementRecord);

    void deleteAll();
}
