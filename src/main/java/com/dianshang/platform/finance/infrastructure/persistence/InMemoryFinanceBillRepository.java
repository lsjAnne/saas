package com.dianshang.platform.finance.infrastructure.persistence;

import com.dianshang.platform.finance.domain.repository.FinanceBillRepository;
import com.dianshang.platform.finance.model.FinanceBill;
import com.dianshang.platform.saas.infrastructure.persistence.JdbcIdCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryFinanceBillRepository implements FinanceBillRepository {

    private final Map<String, FinanceBill> storage = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public List<FinanceBill> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(bill -> storeIds.contains(bill.storeId()))
                .sorted(Comparator.comparing(FinanceBill::createdAt).reversed()
                        .thenComparing(FinanceBill::financeBillId, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public Optional<FinanceBill> findByFinanceBillId(String financeBillId) {
        return Optional.ofNullable(storage.get(financeBillId));
    }

    @Override
    public Optional<FinanceBill> findByStoreAndPeriod(String storeId, String billType, LocalDate periodStart, LocalDate periodEnd) {
        return storage.values().stream()
                .filter(bill -> storeId.equals(bill.storeId())
                        && billType.equals(bill.billType())
                        && periodStart.equals(bill.periodStart())
                        && periodEnd.equals(bill.periodEnd()))
                .findFirst();
    }

    @Override
    public FinanceBill save(FinanceBill financeBill) {
        FinanceBill stored = financeBill;
        if (financeBill.financeBillId() == null || financeBill.financeBillId().isBlank()) {
            stored = new FinanceBill(
                    JdbcIdCodec.formatFinanceBillId(sequence.getAndIncrement()),
                    financeBill.storeId(),
                    financeBill.billType(),
                    financeBill.periodStart(),
                    financeBill.periodEnd(),
                    financeBill.incomeAmount(),
                    financeBill.costAmount(),
                    financeBill.grossProfit(),
                    financeBill.billStatus(),
                    financeBill.createdAt()
            );
        }
        storage.put(stored.financeBillId(), stored);
        return stored;
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(1L);
    }
}
