package com.vieterp.otb.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record AllocationResultDto(
    BigDecimal budgetAmount,
    List<CollectionRecommendation> collections,
    List<GenderRecommendation> genders,
    List<CategoryRecommendation> categories,
    Double overallConfidence,
    List<String> warnings
) {
    public record CollectionRecommendation(
        String name,
        BigDecimal amount,
        Double percentage,
        Double confidence
    ) {}

    public record GenderRecommendation(
        String name,
        BigDecimal amount,
        Double percentage,
        Double confidence
    ) {}

    public record CategoryRecommendation(
        String name,
        BigDecimal amount,
        Double percentage,
        Double confidence
    ) {}
}
