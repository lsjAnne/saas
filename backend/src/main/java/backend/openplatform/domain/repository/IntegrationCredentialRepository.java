package backend.openplatform.domain.repository;

import backend.openplatform.model.IntegrationCredential;

import java.util.Optional;

public interface IntegrationCredentialRepository {

    IntegrationCredential save(IntegrationCredential integrationCredential);

    Optional<IntegrationCredential> findByPluginAppIdAndCredentialType(String pluginAppId, String credentialType);

    Optional<IntegrationCredential> findByAccessKey(String accessKey);

    void deleteAll();
}

