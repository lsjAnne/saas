package com.dianshang.platform.live.infrastructure.persistence;

import com.dianshang.platform.live.domain.repository.LiveScriptRepository;
import com.dianshang.platform.live.model.LiveScript;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryLiveScriptRepository implements LiveScriptRepository {

    private final Map<String, LiveScript> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(11100);

    @Override
    public LiveScript save(LiveScript liveScript) {
        String liveScriptId = liveScript.liveScriptId();
        if (liveScriptId == null || liveScriptId.isBlank()) {
            liveScriptId = "live-script-" + sequence.incrementAndGet();
        }
        LiveScript saved = new LiveScript(
                liveScriptId,
                liveScript.livePlanId(),
                liveScript.productId(),
                liveScript.scriptVersion(),
                liveScript.scriptContent(),
                liveScript.active(),
                liveScript.createdAt()
        );
        storage.put(saved.liveScriptId(), saved);
        return saved;
    }

    @Override
    public List<LiveScript> findByLivePlanId(String livePlanId) {
        return storage.values().stream()
                .filter(script -> livePlanId.equals(script.livePlanId()))
                .sorted(Comparator.comparing(LiveScript::createdAt).reversed())
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(11100);
    }
}
