package backend.order.infrastructure.persistence;

import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderMain;
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
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, OrderMain> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9000);

    @Override
    public OrderMain save(OrderMain orderMain) {
        String orderId = orderMain.orderId();
        if (orderId == null || orderId.isBlank()) {
            orderId = "order-" + sequence.incrementAndGet();
        }
        OrderMain saved = new OrderMain(
                orderId,
                orderMain.storeId(),
                orderMain.platformOrderId(),
                orderMain.orderStatus(),
                orderMain.logisticsStatus(),
                orderMain.totalAmount(),
                orderMain.estimatedProfit(),
                orderMain.buyerName(),
                orderMain.buyerPhoneMask(),
                orderMain.shippingAddress(),
                orderMain.timeoutAt(),
                orderMain.createdAt()
        );
        storage.put(saved.orderId(), saved);
        return saved;
    }

    @Override
    public List<OrderMain> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(orderMain -> storeIds.contains(orderMain.storeId()))
                .sorted(Comparator.comparing(OrderMain::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<OrderMain> findByOrderId(String orderId) {
        return Optional.ofNullable(storage.get(orderId));
    }

    @Override
    public Optional<OrderMain> findByStoreAndPlatformOrderId(String storeId, String platformOrderId) {
        return storage.values().stream()
                .filter(orderMain -> storeId.equals(orderMain.storeId()))
                .filter(orderMain -> platformOrderId.equals(orderMain.platformOrderId()))
                .findFirst();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9000);
    }
}

