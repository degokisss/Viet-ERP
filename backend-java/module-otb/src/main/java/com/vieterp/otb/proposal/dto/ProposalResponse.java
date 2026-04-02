package com.vieterp.otb.proposal.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record ProposalResponse(
    Long id,
    String status,
    Boolean isFinalVersion,
    Integer version,
    Instant createdAt,
    Instant updatedAt,
    CreatorSummary creator,
    AllocateHeaderSummary allocateHeader,
    List<SKUProposalSummary> proposals,
    List<SizingHeaderSummary> sizingHeaders
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
        String brandName,
        String brandCode
    ) {}

    @Builder
    public record SKUProposalSummary(
        Long id,
        Long productId,
        String customerTarget,
        BigDecimal unitCost,
        BigDecimal srp,
        List<AllocateSummary> allocations
    ) {}

    @Builder
    public record AllocateSummary(
        Long id,
        Long storeId,
        BigDecimal quantity
    ) {}

    @Builder
    public record SizingHeaderSummary(
        Long id,
        Integer version,
        Boolean isFinalVersion,
        List<SizingSummary> sizings
    ) {}

    @Builder
    public record SizingSummary(
        Long id,
        Long skuProposalId,
        Long subcategorySizeId,
        BigDecimal actualSalesmixPct,
        BigDecimal actualStPct,
        Integer proposalQuantity
    ) {}
}
