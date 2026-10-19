package backend.channel.infrastructure.persistence;

import backend.channel.domain.repository.ChannelTokenRepository;
import backend.channel.model.ChannelToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class JdbcChannelTokenRepositoryTest {

    @Autowired
    private ChannelTokenRepository channelTokenRepository;

    @BeforeEach
    void setUp() {
        channelTokenRepository.deleteAll();
    }

    @Test
    void shouldWireJdbcImplementation() {
        assertThat(channelTokenRepository).isInstanceOf(JdbcChannelTokenRepository.class);
    }

    @Test
    void shouldInsertFindUpdateAndRoundTrip() {
        OffsetDateTime now = OffsetDateTime.now();
        ChannelToken saved = channelTokenRepository.save(new ChannelToken(
                null, "tenant-1", "org-1", "pdd", "jdbc-pdd-shop",
                "access-token", "refresh-token", "Bearer", "all",
                now.plusDays(7), now.plusDays(30), Map.of("k", "v"), now, now));

        assertThat(saved.channelTokenId()).startsWith("channel-token-");
        ChannelToken found = channelTokenRepository.findByChannelTokenId(saved.channelTokenId()).orElseThrow();
        assertThat(found.channelType()).isEqualTo("pdd");
        assertThat(found.accountName()).isEqualTo("jdbc-pdd-shop");
        assertThat(found.extraConfig()).containsEntry("k", "v");

        ChannelToken updated = new ChannelToken(saved.channelTokenId(), "tenant-1", "org-1", "pdd", "jdbc-pdd-shop",
                "access-token-v2", "refresh-token-v2", "Bearer", "all",
                now.plusDays(7), now.plusDays(30), Map.of("k2", "v2"), found.createdAt(), now);
        channelTokenRepository.save(updated);
        assertThat(channelTokenRepository.findByChannelTokenId(saved.channelTokenId()).orElseThrow().accessToken())
                .isEqualTo("access-token-v2");

        assertThat(channelTokenRepository.findByOrganizationAndTypeAndAccount("org-1", "pdd", "jdbc-pdd-shop"))
                .map(ChannelToken::accessToken)
                .contains("access-token-v2");
        assertThat(channelTokenRepository.findByOrganizationIds(List.of("org-1"))).hasSize(1);
    }

    @Test
    void shouldEnforceUniqueOrganizationTypeAccount() {
        OffsetDateTime now = OffsetDateTime.now();
        channelTokenRepository.save(new ChannelToken(null, "tenant-1", "org-2", "jd", "unique-jd-shop",
                "a", "r", "Bearer", null, now.plusDays(7), now.plusDays(30), Map.of(), now, now));

        assertThatThrownBy(() -> channelTokenRepository.save(new ChannelToken(
                null, "tenant-1", "org-2", "jd", "unique-jd-shop",
                "a2", "r2", "Bearer", null, now.plusDays(7), now.plusDays(30), Map.of(), now, now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
