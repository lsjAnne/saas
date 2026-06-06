package com.dianshang.platform.order.dto;

import com.dianshang.platform.order.model.OrderItem;
import com.dianshang.platform.order.model.OrderMain;

import java.util.List;

public record OrderDetailView(
        OrderMain order,
        List<OrderItem> items
) {
}
