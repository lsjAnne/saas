package com.dianshang.platform.saas.model;

import java.time.OffsetDateTime;
import java.util.Map;

public record TenantProfile(
        String tenantId,
        String tenantCode,
        String tenantName,
        String tenantStatus,
        String ownerName,
        String mobile,
        String defaultOrganizationId,
        Map<String, Boolean> featureFlags,
        OffsetDateTime trialEndAt,
        OffsetDateTime createdAt
) {
    public TenantProfile withStatusAndTrialEndAt(String tenantStatus, OffsetDateTime trialEndAt) {
        return new TenantProfile(
                tenantId,
                tenantCode,
                tenantName,
                tenantStatus,
                ownerName,
                mobile,
                defaultOrganizationId,
                featureFlags,
                trialEndAt,
                createdAt
        );
    }

    public TenantProfile withFeatureFlags(Map<String, Boolean> featureFlags) {
        return new TenantProfile(
                tenantId,
                tenantCode,
                tenantName,
                tenantStatus,
                ownerName,
                mobile,
                defaultOrganizationId,
                featureFlags,
                trialEndAt,
                createdAt
        );
    }
}
