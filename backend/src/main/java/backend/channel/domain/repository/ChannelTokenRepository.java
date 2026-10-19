package backend.channel.domain.repository;

import backend.channel.model.ChannelToken;

import java.util.List;
import java.util.Optional;

public interface ChannelTokenRepository {

    ChannelToken save(ChannelToken token);

    Optional<ChannelToken> findByChannelTokenId(String channelTokenId);

    Optional<ChannelToken> findByOrganizationAndTypeAndAccount(String organizationId, String channelType, String accountName);

    List<ChannelToken> findByOrganizationIds(List<String> organizationIds);

    void deleteAll();
}
