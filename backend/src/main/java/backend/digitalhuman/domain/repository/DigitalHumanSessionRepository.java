package backend.digitalhuman.domain.repository;

import backend.digitalhuman.model.DigitalHumanSession;

import java.util.List;
import java.util.Optional;

public interface DigitalHumanSessionRepository {

    DigitalHumanSession save(DigitalHumanSession session);

    List<DigitalHumanSession> listByTenant(String tenantId);

    List<DigitalHumanSession> listLiveByTenant(String tenantId);

    Optional<DigitalHumanSession> findById(String tenantId, String sessionId);
}
