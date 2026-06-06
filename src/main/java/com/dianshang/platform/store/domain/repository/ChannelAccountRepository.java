package com.dianshang.platform.store.domain.repository;

import com.dianshang.platform.store.model.ChannelAccount;

import java.util.List;
import java.util.Optional;

public interface ChannelAccountRepository {

    ChannelAccount save(ChannelAccount channelAccount);

    List<ChannelAccount> findByOrganizationIds(List<String> organizationIds);

    Optional<ChannelAccount> findByChannelAccountId(String channelAccountId);

    void deleteAll();
}
