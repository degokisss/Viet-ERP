package com.vieterp.tpm.domain.dto;

import com.vieterp.tpm.domain.enums.PromotionStatus;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PromotionResponse(
    UUID id,
    String name,
    String description,
    Instant startDate,
    Instant endDate,
    BigDecimal budget,
    BigDecimal spentAmount,
    PromotionStatus status,
    String customerId,
    String tenantId,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
