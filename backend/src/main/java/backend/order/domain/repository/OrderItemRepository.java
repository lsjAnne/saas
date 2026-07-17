package backend.order.domain.repository;

import backend.order.model.OrderItem;

import java.util.List;

public interface OrderItemRepository {

    OrderItem save(OrderItem orderItem);

    List<OrderItem> findByOrderId(String orderId);

    void deleteAll();
}

