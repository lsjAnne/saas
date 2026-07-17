package backend.store.domain.repository;

import backend.store.model.ChannelAccount;

import java.util.List;
import java.util.Optional;

public interface ChannelAccountRepository {

    ChannelAccount save(ChannelAccount channelAccount);

    List<ChannelAccount> findByOrganizationIds(List<String> organizationIds);

    Optional<ChannelAccount> findByChannelAccountId(String channelAccountId);

    void deleteAll();
}

