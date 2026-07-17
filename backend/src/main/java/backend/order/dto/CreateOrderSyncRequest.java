package backend.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateOrderSyncRequest(
        @NotBlank(message = "storeId is required")
        String storeId,
        String platformOrderId,
        String productId,
        String skuId,
        @Min(value = 1, message = "quantity must be greater than or equal to 1")
        Integer quantity,
        @DecimalMin(value = "0.00", message = "unitPrice must be greater than or equal to 0")
        BigDecimal unitPrice,
        String buyerName,
        String buyerPhoneMask,
        String shippingAddress
) {
}

