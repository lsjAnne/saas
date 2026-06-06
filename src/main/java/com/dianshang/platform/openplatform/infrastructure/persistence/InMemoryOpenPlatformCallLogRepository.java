package com.dianshang.platform.openplatform.infrastructure.persistence;

import com.dianshang.platform.openplatform.domain.repository.OpenPlatformCallLogRepository;
import com.dianshang.platform.openplatform.model.OpenPlatformCallLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryOpenPlatformCallLogRepository implements OpenPlatformCallLogRepository {

    private final Map<String, OpenPlatformCallLog> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(32000);

    @Override
    public OpenPlatformCallLog save(OpenPlatformCallLog callLog) {
        String logId = callLog.logId();
        if (logId == null || logId.isBlank()) {
            logId = "open-log-" + sequence.incrementAndGet();
        }
        OpenPlatformCallLog saved = new OpenPlatformCallLog(
                logId,
                callLog.tenantId(),
                callLog.organizationId(),
                callLog.appId(),
                callLog.subscriptionId(),
                callLog.requestId(),
                callLog.endpoint(),
                callLog.direction(),
                callLog.sourceModule(),
                callLog.resultStatus(),
                callLog.signatureVerified(),
                callLog.replayed(),
                callLog.traceId(),
                callLog.message(),
                callLog.createdAt()
        );
        storage.put(saved.logId(), saved);
        return saved;
    }

    @Override
    public List<OpenPlatformCallLog> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(log -> tenantId.equals(log.tenantId()))
                .sorted(Comparator.comparing(OpenPlatformCallLog::createdAt).thenComparing(OpenPlatformCallLog::logId))
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(32000);
    }
}
