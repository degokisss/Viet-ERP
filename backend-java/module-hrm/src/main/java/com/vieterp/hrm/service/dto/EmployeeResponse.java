package com.vieterp.hrm.service.dto;

import java.time.Instant;
import java.util.UUID;

public record EmployeeResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String phone,
    DepartmentSummary dept,
    Instant hireDate,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
