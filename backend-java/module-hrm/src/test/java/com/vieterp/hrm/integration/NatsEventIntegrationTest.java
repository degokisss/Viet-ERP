package com.vieterp.hrm.integration;

/**
 * NATS Integration Test.
 *
 * Prerequisites:
 * - NATS server running at localhost:4222 (or configured via NATS_URL)
 * - Spring Cloud Stream NATS binder on classpath
 *
 * To run manually:
 * 1. Start NATS: docker run -p 4222:4222 nats:latest
 * 2. Run this test: mvn test -pl module-hrm -Dtest=NatsEventIntegrationTest
 * 3. Publish a message via controller POST /api/v1/hrm/employees
 * 4. Verify message on NATS: nats sub "hrm.employee.created"
 */
class NatsEventIntegrationTest {
    // Integration test placeholder
    // See module-hrm/src/main/resources/application.yml for NATS binding config
    // Topics: hrm.employee.created, hrm.employee.updated, hrm.employee.deleted
}