package com.vieterp.tpm.domain.dto;

import com.vieterp.tpm.domain.enums.PromotionStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record CreatePromotionRequest(
    @NotBlank(message = "Name is required") @Size(max = 255) String name,
    String description,
    @NotNull(message = "Start date is required") Instant startDate,
    @NotNull(message = "End date is required") Instant endDate,
    @NotNull(message = "Budget is required") @DecimalMin(value = "0.0", message = "Budget must be non-negative") BigDecimal budget,
    PromotionStatus status,
    String customerId,
    @NotBlank(message = "Tenant ID is required") String tenantId,
    String createdBy
) {}
