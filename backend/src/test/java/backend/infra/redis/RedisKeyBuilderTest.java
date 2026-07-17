package backend.infra.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisKeyBuilderTest {

    @Test
    void shouldBuildTenantScopedRedisKey() {
        RedisKeyBuilder redisKeyBuilder = new RedisKeyBuilder("dsp");
        assertThat(redisKeyBuilder.buildTenantKey("tenant-1001", "quota", "live_concurrency"))
                .isEqualTo("dsp:tenant-1001:quota:live_concurrency");
        assertThat(redisKeyBuilder.buildIdempotencyKey("tenant-1001", "webhook", "msg-001"))
                .isEqualTo("dsp:tenant-1001:idempotent:webhook:msg-001");
    }
}

class RedisStartupVerifierTest {

    @Test
    void shouldSkipStartupVerificationWhenDisabled() {
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        RedisStartupVerifier verifier = new RedisStartupVerifier(stringRedisTemplate, false);

        verifier.run(new DefaultApplicationArguments(new String[0]));

        verify(stringRedisTemplate, never()).getRequiredConnectionFactory();
    }

    @Test
    void shouldPingRedisAndCloseConnectionWhenVerificationEnabled() {
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(stringRedisTemplate.getRequiredConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        RedisStartupVerifier verifier = new RedisStartupVerifier(stringRedisTemplate, true);

        verifier.run(new DefaultApplicationArguments(new String[0]));

        verify(connection).ping();
        verify(connection).close();
    }

    @Test
    void shouldNotThrowWhenRedisVerificationFails() {
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        when(stringRedisTemplate.getRequiredConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenThrow(new IllegalStateException("redis unavailable"));
        RedisStartupVerifier verifier = new RedisStartupVerifier(stringRedisTemplate, true);

        assertThatCode(() -> verifier.run(new DefaultApplicationArguments(new String[0])))
                .doesNotThrowAnyException();
    }
}

class RedisIdempotencyServiceTest {

    @Test
    void shouldUseTenantScopedKeyWhenMarkingIdempotency() {
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("dsp:tenant-1001:idempotent:webhook:msg-001"),
                eq("1"),
                eq(Duration.ofMinutes(5))))
                .thenReturn(true);
        RedisIdempotencyService service = new RedisIdempotencyService(stringRedisTemplate, new RedisKeyBuilder("dsp"));

        boolean result = service.markIfAbsent("tenant-1001", "webhook", "msg-001", Duration.ofMinutes(5));

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenIdempotencyKeyDoesNotExist() {
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        when(stringRedisTemplate.hasKey("dsp:tenant-1001:idempotent:webhook:msg-001")).thenReturn(false);
        RedisIdempotencyService service = new RedisIdempotencyService(stringRedisTemplate, new RedisKeyBuilder("dsp"));

        boolean result = service.exists("tenant-1001", "webhook", "msg-001");

        assertThat(result).isFalse();
    }
}

