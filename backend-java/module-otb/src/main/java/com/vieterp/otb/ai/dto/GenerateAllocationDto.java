package com.vieterp.otb.ai.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
public class GenerateAllocationDto {
    private BigDecimal budgetAmount;
    private Long storeId;
}
