package com.vieterp.events.employee;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeCreatedEventTest {

    @Test
    void testRecordCreation() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        var event = new EmployeeCreatedEvent(id, "test@example.com", "Nguyen", "Van A", now);
        assertEquals(id, event.employeeId());
        assertEquals("test@example.com", event.email());
        assertEquals("Nguyen", event.firstName());
        assertEquals("Van A", event.lastName());
        assertEquals(now, event.occurredAt());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        var e1 = new EmployeeCreatedEvent(id, "test@example.com", "Nguyen", "Van A", now);
        var e2 = new EmployeeCreatedEvent(id, "test@example.com", "Nguyen", "Van A", now);
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}