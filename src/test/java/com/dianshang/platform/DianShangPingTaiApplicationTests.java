package com.dianshang.platform;

import com.dianshang.platform.saas.application.SaasTenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DianShangPingTaiApplicationTests {

    @Test
    void contextLoads() {
    }
}

@SpringBootTest(
        classes = ProfilePropertiesTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class DefaultRedisConfigurationTest {

    @Autowired
    private RedisProperties redisProperties;

    @Test
    void shouldResolveLocalDockerRedisByDefault() {
        assertThat(redisProperties.getHost()).isEqualTo("127.0.0.1");
        assertThat(redisProperties.getPort()).isEqualTo(6379);
        assertThat(redisProperties.getPassword()).isEqualTo("mypassword");
    }
}

@SpringBootTest(
        classes = ProfilePropertiesTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("postgres")
class PostgreSqlProfileConfigurationTest {

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @Autowired
    private RedisProperties redisProperties;

    @Test
    void shouldResolvePostgreSqlDatasourceWhenPostgresProfileIsActive() {
        assertThat(dataSourceProperties.getDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(dataSourceProperties.getUrl()).contains("jdbc:postgresql://");
    }

    @Test
    void shouldResolveLocalDockerRedisWhenPostgresProfileIsActive() {
        assertThat(redisProperties.getHost()).isEqualTo("127.0.0.1");
        assertThat(redisProperties.getPort()).isEqualTo(6379);
        assertThat(redisProperties.getPassword()).isEqualTo("mypassword");
    }
}

@SpringBootTest(
        classes = ProfilePropertiesTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("tunnel-local")
class TunnelLocalRedisProfileConfigurationTest {

    @Autowired
    private RedisProperties redisProperties;

    @Test
    void shouldResolveLocalDockerRedisWhenTunnelLocalProfileIsActive() {
        assertThat(redisProperties.getHost()).isEqualTo("127.0.0.1");
        assertThat(redisProperties.getPort()).isEqualTo(16379);
        assertThat(redisProperties.getPassword()).isEqualTo("saas_redis_password");
    }
}

@SpringBootTest(
        classes = ProfilePropertiesTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("server-local")
class ServerLocalRedisProfileConfigurationTest {

    @Autowired
    private RedisProperties redisProperties;

    @Test
    void shouldResolveLocalDockerRedisWhenServerLocalProfileIsActive() {
        assertThat(redisProperties.getHost()).isEqualTo("127.0.0.1");
        assertThat(redisProperties.getPort()).isEqualTo(6379);
        assertThat(redisProperties.getPassword()).isEqualTo("mypassword");
    }
}

@SpringBootTest(
        classes = DianShangPingTaiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "APP_STAGE13_LOCAL_HTTP_PORT=18091",
                "APP_STAGE13_LOCAL_TCP_PORT=15691",
                "app.redis.verify-on-startup=false",
                "app.auth.token-secret=test-stage13-local-token-secret-123456",
                "app.auth.bootstrap-password=test-stage13-local-bootstrap-password-123456"
        }
)
@ActiveProfiles("stage13-local")
class Stage13LocalResolvedPlaceholderProbeConfigurationTest {

    @Autowired
    private SaasTenantService saasTenantService;

    @Test
    void shouldResolveStage13LocalPlaceholderBackedEndpointsAsReachable() {
        SaasTenantService.ExternalIntegrationConnectivitySnapshot externalIntegrationConnectivity =
                saasTenantService.getExternalIntegrationConnectivitySnapshot();
        assertThat(externalIntegrationConnectivity.ready()).isTrue();
        assertThat(externalIntegrationConnectivity.configuredCount()).isEqualTo(5);
        assertThat(externalIntegrationConnectivity.reachableCount()).isEqualTo(5);
        assertThat(externalIntegrationConnectivity.erp().host()).isEqualTo("127.0.0.1");
        assertThat(externalIntegrationConnectivity.erp().detail()).isEqualTo("http 200");
        assertThat(externalIntegrationConnectivity.messaging().host()).isEqualTo("127.0.0.1");
        assertThat(externalIntegrationConnectivity.messaging().detail()).isEqualTo("tcp connected");

        SaasTenantService.ObservabilityStackConnectivitySnapshot observabilityStack =
                saasTenantService.getObservabilityStackConnectivitySnapshot();
        assertThat(observabilityStack.logAggregation().host()).isEqualTo("127.0.0.1");
        assertThat(observabilityStack.logAggregation().reachable()).isTrue();
        assertThat(observabilityStack.trace().reachable()).isTrue();
        assertThat(observabilityStack.alertRouter().reachable()).isTrue();
        assertThat(observabilityStack.dashboard().reachable()).isTrue();

        SaasTenantService.DeliveryPipelineSnapshot deliveryPipeline = saasTenantService.getDeliveryPipelineSnapshot();
        assertThat(deliveryPipeline.ready()).isTrue();
        assertThat(deliveryPipeline.githubProbe().host()).isEqualTo("127.0.0.1");
        assertThat(deliveryPipeline.githubProbe().detail()).isEqualTo("http 200");
        assertThat(deliveryPipeline.registryProbe().host()).isEqualTo("127.0.0.1");
        assertThat(deliveryPipeline.registryProbe().detail()).isEqualTo("http 200");
    }
}

@SpringBootTest(
        classes = DianShangPingTaiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/postgres",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.username=username",
                "spring.datasource.password=password",
                "app.redis.verify-on-startup=false",
                "app.auth.token-secret=test-server-local-postgres-token-secret-123456",
                "app.auth.bootstrap-password=test-server-local-postgres-bootstrap-password-123456"
        }
)
@ActiveProfiles("server-local")
@EnabledIfSystemProperty(named = "postgres.smoke.enabled", matches = "true")
class ServerLocalPostgreSqlConnectionSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldConnectToDockerPostgreSqlAndInitializeSchema() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'tenant'",
                Integer.class
        );
        assertThat(tableCount).isEqualTo(1);
    }
}

@Configuration
@EnableConfigurationProperties({DataSourceProperties.class, RedisProperties.class})
class ProfilePropertiesTestConfiguration {
}
