package backend.exceptioncenter.domain.repository;

import backend.exceptioncenter.model.ExceptionTask;

import java.util.List;
import java.util.Optional;

public interface ExceptionTaskRepository {

    ExceptionTask save(ExceptionTask exceptionTask);

    List<ExceptionTask> findByStoreIds(List<String> storeIds);

    Optional<ExceptionTask> findByExceptionTaskId(String exceptionTaskId);

    void deleteAll();
}

