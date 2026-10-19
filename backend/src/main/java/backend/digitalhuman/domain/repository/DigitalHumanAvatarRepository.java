package backend.digitalhuman.domain.repository;

import backend.digitalhuman.model.DigitalHumanAvatar;

import java.util.List;
import java.util.Optional;

public interface DigitalHumanAvatarRepository {

    DigitalHumanAvatar save(DigitalHumanAvatar avatar);

    List<DigitalHumanAvatar> listByTenant(String tenantId);

    Optional<DigitalHumanAvatar> findById(String tenantId, String avatarId);

    void deleteById(String tenantId, String avatarId);
}
