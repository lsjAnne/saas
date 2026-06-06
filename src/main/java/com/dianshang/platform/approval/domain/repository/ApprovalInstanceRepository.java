package com.dianshang.platform.approval.domain.repository;

import com.dianshang.platform.approval.model.ApprovalInstance;

import java.util.List;
import java.util.Optional;

public interface ApprovalInstanceRepository {

    List<ApprovalInstance> findByTenantId(String tenantId);

    Optional<ApprovalInstance> findByApprovalId(String approvalId);

    Optional<ApprovalInstance> findPendingByRelated(String tenantId, String relatedType, String relatedId);

    ApprovalInstance save(ApprovalInstance approvalInstance);

    void deleteAll();
}
