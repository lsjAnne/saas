package backend.servicecase.infrastructure.persistence;

import backend.servicecase.domain.repository.AfterSaleRecordRepository;
import backend.servicecase.model.AfterSaleRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryAfterSaleRecordRepository implements AfterSaleRecordRepository {

    private final Map<String, AfterSaleRecord> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9600);

    @Override
    public AfterSaleRecord save(AfterSaleRecord afterSaleRecord) {
        String afterSaleId = afterSaleRecord.afterSaleId();
        if (afterSaleId == null || afterSaleId.isBlank()) {
            afterSaleId = "after-sale-" + sequence.incrementAndGet();
        }
        AfterSaleRecord saved = new AfterSaleRecord(
                afterSaleId,
                afterSaleRecord.orderId(),
                afterSaleRecord.afterSaleType(),
                afterSaleRecord.reasonText(),
                afterSaleRecord.status(),
                afterSaleRecord.evidenceBlob(),
                afterSaleRecord.createdAt()
        );
        storage.put(saved.afterSaleId(), saved);
        return saved;
    }

    @Override
    public Optional<AfterSaleRecord> findByAfterSaleId(String afterSaleId) {
        return Optional.ofNullable(storage.get(afterSaleId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9600);
    }
}

