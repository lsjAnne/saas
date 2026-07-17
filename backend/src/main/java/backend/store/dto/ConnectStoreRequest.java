package backend.store.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record ConnectStoreRequest(
        @NotBlank(message = "organizationId is required")
        String organizationId,
        @NotBlank(message = "ownerUserId is required")
        String ownerUserId,
        @NotBlank(message = "platformType is required")
        String platformType,
        @NotBlank(message = "platformShopId is required")
        String platformShopId,
        @NotBlank(message = "shopName is required")
        String shopName,
        @NotNull(message = "profitThreshold is required")
        @DecimalMin(value = "0.00", message = "profitThreshold must be greater than or equal to 0")
        BigDecimal profitThreshold,
        @NotNull(message = "riskThreshold is required")
        @DecimalMin(value = "0.00", message = "riskThreshold must be greater than or equal to 0")
        BigDecimal riskThreshold,
        Map<String, Object> defaultShipConfig
) {
}

