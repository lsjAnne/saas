package com.dianshang.platform.openplatform.domain.repository;

import com.dianshang.platform.openplatform.model.OpenPlatformCallLog;

import java.util.List;

public interface OpenPlatformCallLogRepository {

    OpenPlatformCallLog save(OpenPlatformCallLog callLog);

    List<OpenPlatformCallLog> findByTenantId(String tenantId);

    void deleteAll();
}
