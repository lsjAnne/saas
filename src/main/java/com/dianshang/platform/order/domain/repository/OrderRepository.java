package com.dianshang.platform.order.domain.repository;

import com.dianshang.platform.order.model.OrderMain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    OrderMain save(OrderMain orderMain);

    List<OrderMain> findByStoreIds(List<String> storeIds);

    Optional<OrderMain> findByOrderId(String orderId);

    Optional<OrderMain> findByStoreAndPlatformOrderId(String storeId, String platformOrderId);

    void deleteAll();
}
