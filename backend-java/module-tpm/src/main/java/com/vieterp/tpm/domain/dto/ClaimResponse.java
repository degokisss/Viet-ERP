package com.vieterp.tpm.domain.dto;

import com.vieterp.tpm.domain.enums.ClaimStatus;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record ClaimResponse(
    UUID id,
    String claimNumber,
    String claimType,
    BigDecimal amount,
    ClaimStatus status,
    String customerId,
    String promotionId,
    Instant submittedAt,
    Instant approvedAt,
    String tenantId,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
