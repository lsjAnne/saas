package backend.digitalhuman.domain.repository;

import backend.digitalhuman.model.DigitalHumanBarrage;

import java.util.List;

public interface DigitalHumanBarrageRepository {

    DigitalHumanBarrage save(DigitalHumanBarrage barrage);

    List<DigitalHumanBarrage> listByTenant(String tenantId);

    List<DigitalHumanBarrage> listBySession(String tenantId, String sessionId);
}
