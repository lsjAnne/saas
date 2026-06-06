package com.dianshang.platform.openplatform.infrastructure.persistence;

import com.dianshang.platform.openplatform.domain.repository.PluginAppRepository;
import com.dianshang.platform.openplatform.model.PluginApp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryPluginAppRepository implements PluginAppRepository {

    private final Map<String, PluginApp> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(12000);

    @Override
    public PluginApp save(PluginApp pluginApp) {
        String appId = pluginApp.appId();
        if (appId == null || appId.isBlank()) {
            appId = "app-" + sequence.incrementAndGet();
        }
        PluginApp saved = new PluginApp(
                appId,
                pluginApp.organizationId(),
                pluginApp.appName(),
                pluginApp.appType(),
                pluginApp.permissionScope() == null ? List.of() : List.copyOf(pluginApp.permissionScope()),
                pluginApp.accessKey(),
                pluginApp.secretMasked(),
                pluginApp.status(),
                pluginApp.createdAt()
        );
        storage.put(saved.appId(), saved);
        return saved;
    }

    @Override
    public List<PluginApp> findByOrganizationIds(List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        Set<String> organizationIdSet = Set.copyOf(organizationIds);
        return storage.values().stream()
                .filter(pluginApp -> organizationIdSet.contains(pluginApp.organizationId()))
                .sorted(Comparator.comparing(PluginApp::createdAt).thenComparing(PluginApp::appId))
                .toList();
    }

    @Override
    public Optional<PluginApp> findByAppId(String appId) {
        return Optional.ofNullable(storage.get(appId));
    }

    @Override
    public Optional<PluginApp> findByOrganizationIdAndAppName(String organizationId, String appName) {
        return storage.values().stream()
                .filter(pluginApp -> organizationId.equals(pluginApp.organizationId()))
                .filter(pluginApp -> appName.equals(pluginApp.appName()))
                .findFirst();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(12000);
    }
}
