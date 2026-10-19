package backend.channel.dto;

import jakarta.validation.constraints.NotBlank;

public record ShipOrderRequest(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "channelAccountId is required")
        String channelAccountId,
        @NotBlank(message = "platformOrderId is required")
        String platformOrderId,
        @NotBlank(message = "logisticsCompany is required")
        String logisticsCompany,
        @NotBlank(message = "logisticsNo is required")
        String logisticsNo
) {
}
