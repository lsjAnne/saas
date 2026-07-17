package backend.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLogisticsRecordRequest(
        @NotBlank(message = "trackingNumber is required")
        String trackingNumber,
        @NotBlank(message = "logisticsCompany is required")
        String logisticsCompany,
        @NotBlank(message = "logisticsStatus is required")
        String logisticsStatus
) {
}

