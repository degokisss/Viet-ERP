package com.vieterp.otb.budget.domain.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Builder
public record UpdateBudgetRequest(
    String name,

    @DecimalMin("0")
    BigDecimal amount,

    String description
) {}
