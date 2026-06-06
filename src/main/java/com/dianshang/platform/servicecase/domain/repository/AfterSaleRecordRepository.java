package com.dianshang.platform.servicecase.domain.repository;

import com.dianshang.platform.servicecase.model.AfterSaleRecord;

import java.util.Optional;

public interface AfterSaleRecordRepository {

    AfterSaleRecord save(AfterSaleRecord afterSaleRecord);

    Optional<AfterSaleRecord> findByAfterSaleId(String afterSaleId);

    void deleteAll();
}
