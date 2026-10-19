package backend.channel.infrastructure.persistence;

import backend.channel.domain.repository.ChannelAuthStateRepository;
import backend.channel.model.ChannelAuthState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JdbcChannelAuthStateRepositoryTest {

    @Autowired
    private ChannelAuthStateRepository channelAuthStateRepository;

    @BeforeEach
    void setUp() {
        channelAuthStateRepository.deleteAll();
    }

    @Test
    void shouldWireJdbcImplementation() {
        assertThat(channelAuthStateRepository).isInstanceOf(JdbcChannelAuthStateRepository.class);
    }

    @Test
    void shouldInsertFindByStateDeleteByStateAndClear() {
        OffsetDateTime now = OffsetDateTime.now();
        ChannelAuthState saved = channelAuthStateRepository.save(new ChannelAuthState(
                null, "tenant-1", "org-1", "taobao", null,
                "jdbc-state-1", "/api/channels/callback/taobao", now, now.plusMinutes(30)));

        assertThat(saved.authStateId()).startsWith("channel-state-");
        ChannelAuthState found = channelAuthStateRepository.findByState("jdbc-state-1").orElseThrow();
        assertThat(found.channelType()).isEqualTo("taobao");
        assertThat(found.tenantId()).isEqualTo("tenant-1");
        assertThat(found.organizationId()).isEqualTo("org-1");

        channelAuthStateRepository.deleteByState("jdbc-state-1");
        assertThat(channelAuthStateRepository.findByState("jdbc-state-1")).isEmpty();

        channelAuthStateRepository.save(new ChannelAuthState(null, "tenant-1", "org-1", "jd", null,
                "jdbc-state-2", "/callback", now, now.plusMinutes(30)));
        channelAuthStateRepository.deleteAll();
        assertThat(channelAuthStateRepository.findByState("jdbc-state-2")).isEmpty();
    }
}
