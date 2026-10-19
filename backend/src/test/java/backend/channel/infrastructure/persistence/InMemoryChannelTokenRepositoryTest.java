package backend.channel.infrastructure.persistence;

import backend.channel.model.ChannelToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryChannelTokenRepositoryTest {

    private InMemoryChannelTokenRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryChannelTokenRepository();
    }

    @Test
    void shouldGenerateIdAndPersistAndQuery() {
        ChannelToken token = new ChannelToken(null, "tenant-1", "org-1", "pdd", "pdd-shop",
                "access", "refresh", "Bearer", null, OffsetDateTime.now().plusDays(7),
                OffsetDateTime.now().plusDays(30), Map.of(), OffsetDateTime.now(), OffsetDateTime.now());
        ChannelToken saved = repository.save(token);

        assertThat(saved.channelTokenId()).startsWith("channel-token-");
        assertThat(repository.findByChannelTokenId(saved.channelTokenId())).contains(saved);
        assertThat(repository.findByOrganizationAndTypeAndAccount("org-1", "pdd", "pdd-shop")).contains(saved);
        assertThat(repository.findByOrganizationIds(List.of("org-1"))).containsExactly(saved);
        assertThat(repository.findByOrganizationIds(List.of("org-2"))).isEmpty();
    }

    @Test
    void shouldUpdateWhenIdPresentAndClearAll() {
        ChannelToken saved = repository.save(new ChannelToken(null, "tenant-1", "org-1", "jd", "jd-shop",
                "access", "refresh", "Bearer", null, OffsetDateTime.now().plusDays(7),
                OffsetDateTime.now().plusDays(30), Map.of(), OffsetDateTime.now(), OffsetDateTime.now()));
        ChannelToken updated = new ChannelToken(saved.channelTokenId(), "tenant-1", "org-1", "jd", "jd-shop",
                "access-v2", "refresh-v2", "Bearer", null, OffsetDateTime.now().plusDays(7),
                OffsetDateTime.now().plusDays(30), Map.of(), saved.createdAt(), OffsetDateTime.now());
        repository.save(updated);

        assertThat(repository.findByChannelTokenId(saved.channelTokenId()).orElseThrow().accessToken()).isEqualTo("access-v2");
        repository.deleteAll();
        assertThat(repository.findByChannelTokenId(saved.channelTokenId())).isEmpty();
    }
}
