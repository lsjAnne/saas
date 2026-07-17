package backend.auth.model;

import java.time.OffsetDateTime;

public record AuthUser(
        String userId,
        String username,
        String passwordHash,
        String displayName,
        String roleCode,
        String operatorType,
        String tenantId,
        String organizationId,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime passwordUpdatedAt,
        OffsetDateTime lastLoginAt
) {

    public AuthUser withPasswordHash(String nextPasswordHash, OffsetDateTime nextPasswordUpdatedAt) {
        return new AuthUser(
                userId,
                username,
                nextPasswordHash,
                displayName,
                roleCode,
                operatorType,
                tenantId,
                organizationId,
                status,
                createdAt,
                nextPasswordUpdatedAt,
                lastLoginAt
        );
    }

    public AuthUser withLastLoginAt(OffsetDateTime nextLastLoginAt) {
        return new AuthUser(
                userId,
                username,
                passwordHash,
                displayName,
                roleCode,
                operatorType,
                tenantId,
                organizationId,
                status,
                createdAt,
                passwordUpdatedAt,
                nextLastLoginAt
        );
    }

    public AuthUser withOrganizationId(String nextOrganizationId) {
        return new AuthUser(
                userId,
                username,
                passwordHash,
                displayName,
                roleCode,
                operatorType,
                tenantId,
                nextOrganizationId,
                status,
                createdAt,
                passwordUpdatedAt,
                lastLoginAt
        );
    }

    public AuthUser withAccessProfile(String nextDisplayName,
                                      String nextRoleCode,
                                      String nextOperatorType,
                                      String nextTenantId,
                                      String nextOrganizationId) {
        return new AuthUser(
                userId,
                username,
                passwordHash,
                nextDisplayName,
                nextRoleCode,
                nextOperatorType,
                nextTenantId,
                nextOrganizationId,
                status,
                createdAt,
                passwordUpdatedAt,
                lastLoginAt
        );
    }
}

