package com.dianshang.platform.order.domain.repository;

import com.dianshang.platform.order.model.OrderItem;

import java.util.List;

public interface OrderItemRepository {

    OrderItem save(OrderItem orderItem);

    List<OrderItem> findByOrderId(String orderId);

    void deleteAll();
}
