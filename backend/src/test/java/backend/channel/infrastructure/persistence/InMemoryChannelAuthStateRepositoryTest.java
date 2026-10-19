package backend.channel.infrastructure.persistence;

import backend.channel.model.ChannelAuthState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryChannelAuthStateRepositoryTest {

    private InMemoryChannelAuthStateRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryChannelAuthStateRepository();
    }

    @Test
    void shouldSaveFindByStateAndDeleteByState() {
        ChannelAuthState state = new ChannelAuthState(null, "tenant-1", "org-1", "pdd", null,
                "state-abc", "/callback", OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(30));
        ChannelAuthState saved = repository.save(state);

        assertThat(saved.authStateId()).startsWith("channel-state-");
        assertThat(repository.findByState("state-abc")).contains(saved);
        repository.deleteByState("state-abc");
        assertThat(repository.findByState("state-abc")).isEmpty();
    }

    @Test
    void shouldClearAll() {
        repository.save(new ChannelAuthState(null, "tenant-1", "org-1", "pdd", null,
                "state-x", "/callback", OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(30)));
        repository.deleteAll();
        assertThat(repository.findByState("state-x")).isEmpty();
    }
}
