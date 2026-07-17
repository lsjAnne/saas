package backend.servicecase.domain.repository;

import backend.servicecase.model.AfterSaleRecord;

import java.util.Optional;

public interface AfterSaleRecordRepository {

    AfterSaleRecord save(AfterSaleRecord afterSaleRecord);

    Optional<AfterSaleRecord> findByAfterSaleId(String afterSaleId);

    void deleteAll();
}

