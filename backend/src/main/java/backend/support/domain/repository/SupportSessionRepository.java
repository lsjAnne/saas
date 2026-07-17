package backend.support.domain.repository;

import backend.support.model.SupportSession;

import java.util.List;
import java.util.Optional;

public interface SupportSessionRepository {

    SupportSession save(SupportSession supportSession);

    List<SupportSession> findAll();

    Optional<SupportSession> findById(String id);

    void deleteAll();
}

