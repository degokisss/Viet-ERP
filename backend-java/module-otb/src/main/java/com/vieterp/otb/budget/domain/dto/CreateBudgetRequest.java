package com.vieterp.otb.budget.domain.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Builder
public record CreateBudgetRequest(
    @NotBlank(message = "name is required")
    String name,

    @NotNull @DecimalMin("0")
    BigDecimal amount,

    @NotNull @Min(2000) @Max(2100)
    Integer fiscalYear,

    String description,

    String brandId,

    List<BudgetAllocateDto> allocations,

    Boolean isFinalVersion
) {}
