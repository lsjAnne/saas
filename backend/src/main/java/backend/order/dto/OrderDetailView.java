package backend.order.dto;

import backend.order.model.OrderItem;
import backend.order.model.OrderMain;

import java.util.List;

public record OrderDetailView(
        OrderMain order,
        List<OrderItem> items
) {
}

