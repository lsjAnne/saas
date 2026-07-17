package backend.fulfillment.domain.repository;

import backend.fulfillment.model.LogisticsRecord;

import java.util.List;

public interface LogisticsRecordRepository {

    LogisticsRecord save(LogisticsRecord logisticsRecord);

    List<LogisticsRecord> findByFulfillmentTaskId(String fulfillmentTaskId);

    void deleteAll();
}

