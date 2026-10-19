package backend.digitalhuman.domain.repository;

import backend.digitalhuman.model.DigitalHumanScript;

import java.util.List;
import java.util.Optional;

public interface DigitalHumanScriptRepository {

    DigitalHumanScript save(DigitalHumanScript script);

    List<DigitalHumanScript> listByTenant(String tenantId);

    Optional<DigitalHumanScript> findById(String tenantId, String scriptId);
}
