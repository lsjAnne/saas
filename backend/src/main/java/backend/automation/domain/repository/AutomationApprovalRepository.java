package backend.automation.domain.repository;

import backend.automation.model.AutomationApproval;

import java.util.List;
import java.util.Optional;

public interface AutomationApprovalRepository {

    AutomationApproval save(AutomationApproval approval);

    List<AutomationApproval> findByTenantId(String tenantId);

    Optional<AutomationApproval> findByTaskId(String taskId);

    void deleteAll();
}
