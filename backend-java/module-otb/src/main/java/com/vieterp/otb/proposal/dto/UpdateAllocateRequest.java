package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Builder
public record UpdateAllocateRequest(
    @NotNull @DecimalMin("0")
    BigDecimal quantity
) {}
