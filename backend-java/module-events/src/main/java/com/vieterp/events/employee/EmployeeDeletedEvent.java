package com.vieterp.events.employee;

import java.time.Instant;
import java.util.UUID;

public record EmployeeDeletedEvent(
    UUID employeeId,
    Instant occurredAt
) {}
