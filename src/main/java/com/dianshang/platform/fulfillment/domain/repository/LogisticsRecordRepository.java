package com.dianshang.platform.fulfillment.domain.repository;

import com.dianshang.platform.fulfillment.model.LogisticsRecord;

import java.util.List;

public interface LogisticsRecordRepository {

    LogisticsRecord save(LogisticsRecord logisticsRecord);

    List<LogisticsRecord> findByFulfillmentTaskId(String fulfillmentTaskId);

    void deleteAll();
}
