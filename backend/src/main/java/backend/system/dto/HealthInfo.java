package backend.system.dto;

public record HealthInfo(
        String application,
        String status,
        String tenantId
) {
}

