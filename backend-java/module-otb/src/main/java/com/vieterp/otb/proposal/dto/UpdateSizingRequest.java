package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Builder
public record UpdateSizingRequest(
    BigDecimal actualSalesmixPct,
    BigDecimal actualStPct,
    Integer proposalQuantity
) {}
