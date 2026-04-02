package com.vieterp.otb.planning.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record PlanningResponse(
    Long id,
    Integer version,
    String status,
    Boolean isFinalVersion,
    Instant createdAt,
    Instant updatedAt,
    CreatorSummary creator,
    AllocateHeaderSummary allocateHeader,
    List<CollectionSummary> collections,
    List<GenderSummary> genders,
    List<CategorySummary> categories
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
        BrandSummary brand
    ) {}

    @Builder
    public record BrandSummary(
        Long id,
        String code,
        String name,
        String groupBrandName
    ) {}

    @Builder
    public record CollectionSummary(
        Long id,
        Long seasonTypeId,
        String seasonTypeName,
        Long storeId,
        String storeName,
        BigDecimal actualBuyPct,
        BigDecimal actualSalesPct,
        BigDecimal actualStPct,
        BigDecimal actualMoc,
        BigDecimal proposedBuyPct,
        BigDecimal otbProposedAmount,
        BigDecimal pctVarVsLast
    ) {}

    @Builder
    public record GenderSummary(
        Long id,
        Long genderId,
        String genderName,
        Long storeId,
        String storeName,
        BigDecimal actualBuyPct,
        BigDecimal actualSalesPct,
        BigDecimal actualStPct,
        BigDecimal proposedBuyPct,
        BigDecimal otbProposedAmount,
        BigDecimal pctVarVsLast
    ) {}

    @Builder
    public record CategorySummary(
        Long id,
        Long subcategoryId,
        String subcategoryName,
        Long categoryId,
        String categoryName,
        Long genderId,
        String genderName,
        BigDecimal actualBuyPct,
        BigDecimal actualSalesPct,
        BigDecimal actualStPct,
        BigDecimal proposedBuyPct,
        BigDecimal otbProposedAmount,
        BigDecimal varLastyearPct,
        BigDecimal otbActualAmount,
        BigDecimal otbActualBuyPct
    ) {}

    // ─── List item (header-only, no nested details) ────────────────────────────

    @Builder
    public record HeaderSummary(
        Long id,
        Integer version,
        String status,
        Boolean isFinalVersion,
        Instant createdAt,
        CreatorSummary creator,
        AllocateHeaderSummary allocateHeader,
        Integer collectionCount,
        Integer genderCount,
        Integer categoryCount
    ) {}
}
