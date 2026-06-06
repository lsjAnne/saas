package com.dianshang.platform.support.model;

import java.time.OffsetDateTime;

public record SupportSession(
        String id,
        String tenantId,
        String requesterId,
        String reason,
        String approver,
        OffsetDateTime expiresAt,
        String status,
        String approvalRemark,
        boolean active,
        OffsetDateTime createdAt
) {

    public SupportSession approve(String approver, OffsetDateTime expiresAt, String approvalRemark) {
        return new SupportSession(
                id,
                tenantId,
                requesterId,
                reason,
                approver,
                expiresAt,
                "approved",
                approvalRemark,
                true,
                createdAt
        );
    }

    public SupportSession reject(String approver, String approvalRemark) {
        return new SupportSession(
                id,
                tenantId,
                requesterId,
                reason,
                approver,
                expiresAt,
                "rejected",
                approvalRemark,
                false,
                createdAt
        );
    }

    public SupportSession close() {
        return new SupportSession(
                id,
                tenantId,
                requesterId,
                reason,
                approver,
                expiresAt,
                "closed",
                approvalRemark,
                false,
                createdAt
        );
    }
}
