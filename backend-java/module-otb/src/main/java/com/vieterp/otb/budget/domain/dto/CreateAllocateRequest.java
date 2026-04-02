package com.vieterp.otb.budget.domain.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Builder
public record CreateAllocateRequest(
    @NotBlank(message = "brandId is required")
    String brandId,

    @NotEmpty(message = "allocations required")
    List<BudgetAllocateDto> allocations,

    Boolean isFinalVersion
) {}
