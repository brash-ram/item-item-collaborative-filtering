package com.brash;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

/**
 * Конфигурация тестирования в тест-контейнере
 */
@ContextConfiguration(classes = IntegrationEnvironment.JpaIntegrationEnvironmentConfiguration.class)
public abstract class IntegrationEnvironment {

    private static final DataSource TEST_DATA_SOURCE;

    private static final PostgreSQLContainer POSTGRESQL_CONTAINER;


    static {
        String username = "postgres";
        String password = "qwerty";
        String dbName = "postgres";
        Integer dbPort = 5432;
        POSTGRESQL_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
            .withDatabaseName(dbName)
            .withUsername(username)
            .withPassword(password)
            .withExposedPorts(dbPort);
        POSTGRESQL_CONTAINER.start();
        TEST_DATA_SOURCE = DataSourceBuilder.create()
            .url(POSTGRESQL_CONTAINER.getJdbcUrl())
            .username(POSTGRESQL_CONTAINER.getUsername())
            .password(POSTGRESQL_CONTAINER.getPassword())
            .build();
    }

    @DynamicPropertySource
    static void jdbcProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
    }

    @Configuration
    public static class JpaIntegrationEnvironmentConfiguration {
        @Bean
        public DataSource dataSource() {
            return TEST_DATA_SOURCE;
        }
    }

}
