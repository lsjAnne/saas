package backend.digitalhuman.domain.repository;

import backend.digitalhuman.model.DigitalHumanInteraction;

import java.util.List;

public interface DigitalHumanInteractionRepository {

    DigitalHumanInteraction save(DigitalHumanInteraction interaction);

    List<DigitalHumanInteraction> listByTenant(String tenantId);

    List<DigitalHumanInteraction> listBySession(String tenantId, String sessionId);
}
