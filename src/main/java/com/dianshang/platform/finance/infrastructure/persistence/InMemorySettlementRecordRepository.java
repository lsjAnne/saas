package com.dianshang.platform.finance.infrastructure.persistence;

import com.dianshang.platform.finance.domain.repository.SettlementRecordRepository;
import com.dianshang.platform.finance.model.SettlementRecord;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemorySettlementRecordRepository implements SettlementRecordRepository {

    private final Map<String, SettlementRecord> storage = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public List<SettlementRecord> findByFinanceBillId(String financeBillId) {
        return storage.values().stream()
                .filter(record -> financeBillId.equals(record.financeBillId()))
                .sorted(Comparator.comparing(SettlementRecord::createdAt).thenComparing(SettlementRecord::settlementRecordId))
                .toList();
    }

    @Override
    public SettlementRecord save(SettlementRecord settlementRecord) {
        SettlementRecord stored = settlementRecord;
        if (settlementRecord.settlementRecordId() == null || settlementRecord.settlementRecordId().isBlank()) {
            stored = new SettlementRecord(
                    JdbcIdCodec.formatSettlementRecordId(sequence.getAndIncrement()),
                    settlementRecord.financeBillId(),
                    settlementRecord.settlementType(),
                    settlementRecord.settlementAmount(),
                    settlementRecord.settlementStatus(),
                    settlementRecord.settledAt(),
                    settlementRecord.createdAt()
            );
        }
        storage.put(stored.settlementRecordId(), stored);
        return stored;
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(1L);
    }
}
