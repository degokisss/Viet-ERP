package com.vieterp.hrm.service.dto;

import jakarta.validation.constraints.*;

public record CreateEmployeeRequest(
    @NotBlank(message = "First name is required") @Size(max = 100) String firstName,
    @NotBlank(message = "Last name is required") @Size(max = 100) String lastName,
    @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
    String phone,
    Long departmentId,
    String status
) {}
