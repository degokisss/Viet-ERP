package com.vieterp.events.employee;

import java.time.Instant;
import java.util.UUID;

public record EmployeeCreatedEvent(
    UUID employeeId,
    String email,
    String firstName,
    String lastName,
    Instant occurredAt
) {}
