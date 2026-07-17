package backend.openplatform.infrastructure.persistence;

import backend.openplatform.domain.repository.IntegrationCredentialRepository;
import backend.openplatform.model.IntegrationCredential;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryIntegrationCredentialRepository implements IntegrationCredentialRepository {

    private final Map<String, IntegrationCredential> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(30000);

    @Override
    public IntegrationCredential save(IntegrationCredential integrationCredential) {
        String credentialId = integrationCredential.credentialId();
        if (credentialId == null || credentialId.isBlank()) {
            credentialId = "integration-credential-" + sequence.incrementAndGet();
        }
        IntegrationCredential saved = new IntegrationCredential(
                credentialId,
                integrationCredential.pluginAppId(),
                integrationCredential.credentialType(),
                integrationCredential.accessKey(),
                integrationCredential.secretDigest(),
                integrationCredential.secretKeyMasked(),
                integrationCredential.expiresAt(),
                integrationCredential.createdAt()
        );
        storage.put(saved.credentialId(), saved);
        return saved;
    }

    @Override
    public Optional<IntegrationCredential> findByPluginAppIdAndCredentialType(String pluginAppId, String credentialType) {
        return storage.values().stream()
                .filter(credential -> pluginAppId.equals(credential.pluginAppId()))
                .filter(credential -> credentialType.equals(credential.credentialType()))
                .findFirst();
    }

    @Override
    public Optional<IntegrationCredential> findByAccessKey(String accessKey) {
        return storage.values().stream()
                .filter(credential -> accessKey.equals(credential.accessKey()))
                .findFirst();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(30000);
    }
}

