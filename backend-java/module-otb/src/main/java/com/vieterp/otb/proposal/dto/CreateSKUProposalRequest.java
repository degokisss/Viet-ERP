package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Builder
public record CreateSKUProposalRequest(
    @NotBlank(message = "productId is required")
    String productId,

    @NotBlank(message = "customerTarget is required")
    String customerTarget,

    @NotNull @DecimalMin("0")
    BigDecimal unitCost,

    @NotNull @DecimalMin("0")
    BigDecimal srp
) {}
