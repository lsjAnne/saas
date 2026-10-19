package backend.channel.domain.repository;

import backend.channel.model.ChannelAuthState;

import java.util.Optional;

public interface ChannelAuthStateRepository {

    ChannelAuthState save(ChannelAuthState authState);

    Optional<ChannelAuthState> findByState(String state);

    void deleteByState(String state);

    void deleteAll();
}
