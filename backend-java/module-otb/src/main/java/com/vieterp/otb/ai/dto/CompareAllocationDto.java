package com.vieterp.otb.ai.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CompareAllocationDto {
    private List<UserAllocationItem> userAllocation;
    private BigDecimal budgetAmount;

    @Data
    public static class UserAllocationItem {
        private String category;
        private BigDecimal amount;
        private Double percentage;
    }
}
