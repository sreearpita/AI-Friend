package com.example.demo.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.example.demo.support.DockerConditions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("com.example.demo.support.DockerConditions#isDockerAvailable")
class PostgresMigrationIntegrationTest {
    private static final Set<String> REQUIRED_TABLES = Set.of(
            "tenants",
            "tenant_tool_configs",
            "content_sources",
            "content_chunks");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("aifriend")
            .withUsername("aifriend")
            .withPassword("aifriend");

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    static void startContainer() {
        if (!DockerConditions.isDockerAvailable()) {
            return;
        }
        postgres.start();
    }

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        if (!DockerConditions.isDockerAvailable()) {
            return;
        }
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("aif.security.seed-demo-tenant", () -> "true");
        registry.add("aif.tools.seed-demo-tools", () -> "false");
    }

    @Test
    void flywayCreatesCorePlatformTables() throws SQLException {
        Set<String> tables = loadPublicTables();
        assertThat(tables).containsAll(REQUIRED_TABLES);
    }

    @Test
    void seededWellnessContentIsPresent() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                ResultSet resultSet = connection.createStatement().executeQuery("""
                        SELECT COUNT(*) AS chunk_count
                        FROM content_chunks
                        WHERE active = TRUE
                        """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getLong("chunk_count")).isGreaterThan(0);
        }
    }

    private Set<String> loadPublicTables() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                ResultSet resultSet = connection.createStatement().executeQuery("""
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                        """)) {
            return Stream.generate(() -> {
                        try {
                            if (!resultSet.next()) {
                                return null;
                            }
                            return resultSet.getString("table_name");
                        } catch (SQLException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .takeWhile(name -> name != null)
                    .collect(Collectors.toSet());
        }
    }
}
