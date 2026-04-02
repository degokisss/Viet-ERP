package com.vieterp.otb;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @BeforeAll
    static void createDatabaseIfNotExists() {
        // External PostgreSQL at localhost:5432 is assumed to be running.
        // The test database 'vieterp_test' should already exist or Hibernate will
        // fail to connect. Ensure PostgreSQL is started via docker compose or the
        // existing backend-java PostgreSQL container before running integration tests.
    }
}
