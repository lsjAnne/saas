package backend.openplatform.domain.repository;

import backend.openplatform.model.OpenPlatformCallLog;

import java.util.List;

public interface OpenPlatformCallLogRepository {

    OpenPlatformCallLog save(OpenPlatformCallLog callLog);

    List<OpenPlatformCallLog> findByTenantId(String tenantId);

    void deleteAll();
}

