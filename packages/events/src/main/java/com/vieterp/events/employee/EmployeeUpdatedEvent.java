package com.vieterp.events.employee;

import java.time.Instant;
import java.util.UUID;

public record EmployeeUpdatedEvent(
    UUID employeeId,
    String firstName,
    String lastName,
    String email,
    Instant occurredAt
) {}
