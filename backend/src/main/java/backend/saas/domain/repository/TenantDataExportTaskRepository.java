package backend.saas.domain.repository;

import backend.saas.model.TenantDataExportTaskRecord;

import java.util.List;
import java.util.Optional;

public interface TenantDataExportTaskRepository {

    TenantDataExportTaskRecord save(TenantDataExportTaskRecord task);

    List<TenantDataExportTaskRecord> findByTenantId(String tenantId);

    Optional<TenantDataExportTaskRecord> findByTaskId(String exportTaskId);

    void deleteAll();
}

