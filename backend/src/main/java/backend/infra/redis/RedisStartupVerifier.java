package backend.infra.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisStartupVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisStartupVerifier.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final boolean verifyOnStartup;

    public RedisStartupVerifier(StringRedisTemplate stringRedisTemplate,
                                @Value("${app.redis.verify-on-startup:false}") boolean verifyOnStartup) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.verifyOnStartup = verifyOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!verifyOnStartup) {
            return;
        }
        try (RedisConnection connection = stringRedisTemplate.getRequiredConnectionFactory().getConnection()) {
            String pong = connection.ping();
            log.info("Redis startup verify result: {}", pong);
        } catch (Exception exception) {
            log.warn("Redis startup verify failed: {}", exception.getMessage());
        }
    }
}

