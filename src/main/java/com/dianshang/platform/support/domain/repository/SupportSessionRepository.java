package com.dianshang.platform.support.domain.repository;

import com.dianshang.platform.support.model.SupportSession;

import java.util.List;
import java.util.Optional;

public interface SupportSessionRepository {

    SupportSession save(SupportSession supportSession);

    List<SupportSession> findAll();

    Optional<SupportSession> findById(String id);

    void deleteAll();
}
