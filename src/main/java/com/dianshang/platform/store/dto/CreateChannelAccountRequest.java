package com.dianshang.platform.store.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CreateChannelAccountRequest(
        @NotBlank(message = "organizationId is required")
        String organizationId,
        @NotBlank(message = "channelType is required")
        String channelType,
        @NotBlank(message = "accountName is required")
        String accountName,
        Map<String, Object> extraConfig
) {
}
