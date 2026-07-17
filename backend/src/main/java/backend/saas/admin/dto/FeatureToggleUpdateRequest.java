package backend.saas.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record FeatureToggleUpdateRequest(
        @NotNull(message = "featureFlags is required")
        Map<String, Boolean> featureFlags
) {
}

