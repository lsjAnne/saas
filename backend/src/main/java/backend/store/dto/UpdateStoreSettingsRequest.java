package backend.store.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateStoreSettingsRequest(
        @NotNull(message = "profitThreshold is required")
        @DecimalMin(value = "0.00", message = "profitThreshold must be greater than or equal to 0")
        BigDecimal profitThreshold,
        @NotNull(message = "riskThreshold is required")
        @DecimalMin(value = "0.00", message = "riskThreshold must be greater than or equal to 0")
        BigDecimal riskThreshold,
        Map<String, Object> defaultShipConfig
) {
}

