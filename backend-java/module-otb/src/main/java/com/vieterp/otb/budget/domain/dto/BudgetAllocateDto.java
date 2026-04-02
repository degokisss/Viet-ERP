package com.vieterp.otb.budget.domain.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
public class BudgetAllocateDto {
    @NotBlank(message = "storeId is required")
    private String storeId;

    @NotBlank(message = "seasonGroupId is required")
    private String seasonGroupId;

    @NotBlank(message = "seasonId is required")
    private String seasonId;

    @NotNull @DecimalMin("0")
    private BigDecimal budgetAmount;
}
