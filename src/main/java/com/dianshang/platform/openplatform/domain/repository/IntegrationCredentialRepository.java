package com.dianshang.platform.openplatform.domain.repository;

import com.dianshang.platform.openplatform.model.IntegrationCredential;

import java.util.Optional;

public interface IntegrationCredentialRepository {

    IntegrationCredential save(IntegrationCredential integrationCredential);

    Optional<IntegrationCredential> findByPluginAppIdAndCredentialType(String pluginAppId, String credentialType);

    Optional<IntegrationCredential> findByAccessKey(String accessKey);

    void deleteAll();
}
