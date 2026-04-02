package com.vieterp.otb;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base integration test using the external PostgreSQL at localhost:5432.
 * Requires PostgreSQL to be running (via docker compose or system install)
 * and the 'vieterp_test' database to exist.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Uses vieterp_test database on the existing PostgreSQL instance
        registry.add("spring.datasource.url", () ->
            "jdbc:postgresql://localhost:5432/vieterp_test");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Disable Redis for integration tests
        registry.add("spring.cache.type", () -> "none");
    }
}
