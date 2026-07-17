package backend.openplatform.domain.repository;

import backend.openplatform.model.PluginApp;

import java.util.List;
import java.util.Optional;

public interface PluginAppRepository {

    PluginApp save(PluginApp pluginApp);

    List<PluginApp> findByOrganizationIds(List<String> organizationIds);

    Optional<PluginApp> findByAppId(String appId);

    Optional<PluginApp> findByOrganizationIdAndAppName(String organizationId, String appName);

    void deleteAll();
}

