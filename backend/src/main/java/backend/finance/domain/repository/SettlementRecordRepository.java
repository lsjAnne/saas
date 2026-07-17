package backend.finance.domain.repository;

import backend.finance.model.SettlementRecord;

import java.util.List;

public interface SettlementRecordRepository {

    List<SettlementRecord> findByFinanceBillId(String financeBillId);

    SettlementRecord save(SettlementRecord settlementRecord);

    void deleteAll();
}

