package com.vieterp.tpm.domain.dto;

import com.vieterp.tpm.domain.enums.ClaimStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record CreateClaimRequest(
    @NotBlank(message = "Claim number is required") @Size(max = 100) String claimNumber,
    @NotBlank(message = "Claim type is required") @Size(max = 50) String claimType,
    @NotNull(message = "Amount is required") @DecimalMin(value = "0.0", message = "Amount must be non-negative") BigDecimal amount,
    ClaimStatus status,
    String customerId,
    String promotionId,
    Instant submittedAt,
    Instant approvedAt,
    @NotBlank(message = "Tenant ID is required") String tenantId,
    String createdBy
) {}
