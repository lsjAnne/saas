package com.dianshang.platform.order.infrastructure.persistence;

import com.dianshang.platform.order.domain.repository.OrderItemRepository;
import com.dianshang.platform.order.model.OrderItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryOrderItemRepository implements OrderItemRepository {

    private final Map<String, OrderItem> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9100);

    @Override
    public OrderItem save(OrderItem orderItem) {
        String orderItemId = orderItem.orderItemId();
        if (orderItemId == null || orderItemId.isBlank()) {
            orderItemId = "order-item-" + sequence.incrementAndGet();
        }
        OrderItem saved = new OrderItem(
                orderItemId,
                orderItem.orderId(),
                orderItem.productId(),
                orderItem.skuId(),
                orderItem.quantity(),
                orderItem.unitPrice(),
                orderItem.createdAt()
        );
        storage.put(saved.orderItemId(), saved);
        return saved;
    }

    @Override
    public List<OrderItem> findByOrderId(String orderId) {
        return storage.values().stream()
                .filter(orderItem -> orderId.equals(orderItem.orderId()))
                .sorted(Comparator.comparing(OrderItem::createdAt))
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9100);
    }
}
