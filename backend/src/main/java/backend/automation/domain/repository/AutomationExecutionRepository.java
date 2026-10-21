package backend.automation.domain.repository;

import backend.automation.model.AutomationExecution;

import java.util.List;
import java.util.Optional;

public interface AutomationExecutionRepository {

    AutomationExecution save(AutomationExecution execution);

    List<AutomationExecution> findByTenantId(String tenantId);

    Optional<AutomationExecution> findByExecutionId(String executionId);

    /** 幂等去重：同一 目标+规则+动作 已存在执行记录则不再重复执行 */
    Optional<AutomationExecution> findExisting(String tenantId, String targetId, String ruleId, String actionType);

    void deleteAll();
}
