package com.dianshang.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
        classes = PostgreSqlProfileConfigurationTest.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("postgres")
class PostgreSqlProfileConfigurationTest {

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @Test
    void shouldResolvePostgreSqlDatasourceWhenPostgresProfileIsActive() {
        assertThat(dataSourceProperties.getDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(dataSourceProperties.getUrl()).contains("jdbc:postgresql://");
    }

    @Configuration
    @EnableConfigurationProperties(DataSourceProperties.class)
    static class TestConfiguration {
    }
}

@SpringBootTest(
        classes = DianShangPingTaiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/dian_shang_ping_tai",
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
