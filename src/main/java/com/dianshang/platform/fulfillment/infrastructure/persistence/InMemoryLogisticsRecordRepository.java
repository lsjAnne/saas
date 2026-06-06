package com.dianshang.platform.fulfillment.infrastructure.persistence;

import com.dianshang.platform.fulfillment.domain.repository.LogisticsRecordRepository;
import com.dianshang.platform.fulfillment.model.LogisticsRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryLogisticsRecordRepository implements LogisticsRecordRepository {

    private final Map<String, LogisticsRecord> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9300);

    @Override
    public LogisticsRecord save(LogisticsRecord logisticsRecord) {
        String logisticsRecordId = logisticsRecord.logisticsRecordId();
        if (logisticsRecordId == null || logisticsRecordId.isBlank()) {
            logisticsRecordId = "logistics-" + sequence.incrementAndGet();
        }
        LogisticsRecord saved = new LogisticsRecord(
                logisticsRecordId,
                logisticsRecord.fulfillmentTaskId(),
                logisticsRecord.trackingNumber(),
                logisticsRecord.logisticsCompany(),
                logisticsRecord.logisticsStatus(),
                logisticsRecord.syncedAt(),
                logisticsRecord.createdAt()
        );
        storage.put(saved.logisticsRecordId(), saved);
        return saved;
    }

    @Override
    public List<LogisticsRecord> findByFulfillmentTaskId(String fulfillmentTaskId) {
        return storage.values().stream()
                .filter(record -> fulfillmentTaskId.equals(record.fulfillmentTaskId()))
                .sorted(Comparator.comparing(LogisticsRecord::createdAt))
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9300);
    }
}
