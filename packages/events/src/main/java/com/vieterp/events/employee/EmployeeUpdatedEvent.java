package com.vieterp.events.employee;

import java.time.Instant;
import java.util.UUID;

public record EmployeeUpdatedEvent(
    UUID employeeId,
    String email,
    Instant occurredAt
) {}
