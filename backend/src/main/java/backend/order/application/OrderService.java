package backend.order.application;

import backend.order.dto.CreateOrderSyncRequest;
import backend.order.dto.OrderDetailView;
import backend.order.dto.OrderSyncResult;
import backend.order.model.OrderMain;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Deprecated(forRemoval = false)
public class OrderService {

    private final OrderApplicationService orderApplicationService;

    public OrderService(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    public List<OrderMain> listOrders(String tenantId) {
        return orderApplicationService.listOrders(tenantId);
    }

    public OrderDetailView getOrder(String tenantId, String orderId) {
        return orderApplicationService.getOrder(tenantId, orderId);
    }

    public OrderSyncResult syncOrder(String tenantId, CreateOrderSyncRequest request) {
        return orderApplicationService.syncOrder(tenantId, request);
    }

    public void clear() {
        orderApplicationService.clear();
    }
}

