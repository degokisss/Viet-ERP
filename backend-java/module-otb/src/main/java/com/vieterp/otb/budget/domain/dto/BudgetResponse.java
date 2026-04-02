package com.vieterp.otb.budget.domain.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record BudgetResponse(
    Long id,
    String name,
    BigDecimal amount,
    String description,
    String status,
    Integer fiscalYear,
    Instant createdAt,
    Instant updatedAt,
    CreatorSummary creator,
    List<AllocateHeaderSummary> allocateHeaders
) {
    @Builder
    public record CreatorSummary(
        Long id,
        String name,
        String email
    ) {}

    @Builder
    public record AllocateHeaderSummary(
        Long id,
        Integer version,
        Boolean isFinalVersion,
        Boolean isSnapshot,
        BrandSummary brand,
        List<BudgetAllocateSummary> budgetAllocates
    ) {}

    @Builder
    public record BrandSummary(
        Long id,
        String code,
        String name,
        String groupBrandName
    ) {}

    @Builder
    public record BudgetAllocateSummary(
        Long id,
        Long storeId,
        Long seasonGroupId,
        Long seasonId,
        BigDecimal budgetAmount
    ) {}
}
