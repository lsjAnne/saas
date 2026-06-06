package com.dianshang.platform.saas.infrastructure.persistence;

import com.dianshang.platform.saas.domain.repository.BillingOrderRepository;
import com.dianshang.platform.saas.model.BillingOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryBillingOrderRepository implements BillingOrderRepository {

    private final Map<String, BillingOrder> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(5000);

    @Override
    public BillingOrder save(BillingOrder billingOrder) {
        String billingOrderId = billingOrder.billingOrderId();
        if (billingOrderId == null || billingOrderId.isBlank()) {
            billingOrderId = JdbcIdCodec.formatBillingOrderId(sequence.incrementAndGet());
        }
        BillingOrder saved = new BillingOrder(
                billingOrderId,
                billingOrder.tenantId(),
                billingOrder.planCode(),
                billingOrder.planName(),
                billingOrder.orderType(),
                billingOrder.payableAmount(),
                billingOrder.paymentStatus(),
                billingOrder.externalOrderNo(),
                billingOrder.paidAt(),
                billingOrder.createdAt()
        );
        storage.put(saved.billingOrderId(), saved);
        return saved;
    }

    @Override
    public List<BillingOrder> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(order -> tenantId.equals(order.tenantId()))
                .sorted(Comparator.comparing(BillingOrder::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<BillingOrder> findByBillingOrderId(String billingOrderId) {
        return Optional.ofNullable(storage.get(billingOrderId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(5000);
    }
}
