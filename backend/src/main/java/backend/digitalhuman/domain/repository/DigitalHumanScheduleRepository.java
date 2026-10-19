package backend.digitalhuman.domain.repository;

import backend.digitalhuman.model.DigitalHumanSchedule;

import java.util.List;
import java.util.Optional;

public interface DigitalHumanScheduleRepository {

    DigitalHumanSchedule save(DigitalHumanSchedule schedule);

    List<DigitalHumanSchedule> listByTenant(String tenantId);

    Optional<DigitalHumanSchedule> findById(String tenantId, String scheduleId);
}
