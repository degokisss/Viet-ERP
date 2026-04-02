package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Builder
public record UpdateSKUProposalRequest(
    String customerTarget,

    @DecimalMin("0")
    BigDecimal unitCost,

    @DecimalMin("0")
    BigDecimal srp
) {}
