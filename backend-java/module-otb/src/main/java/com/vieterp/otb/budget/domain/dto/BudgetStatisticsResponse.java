package com.vieterp.otb.budget.domain.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Builder
public record BudgetStatisticsResponse(
    long totalBudgets,
    BigDecimal totalAmount,
    BigDecimal approvedAmount,
    Map<String, Long> byStatus
) {}
