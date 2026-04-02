package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
public class FullAllocationDto {
    @NotBlank(message = "skuProposalId is required")
    private String skuProposalId;

    @NotBlank(message = "storeId is required")
    private String storeId;

    @NotNull @DecimalMin("0")
    private BigDecimal quantity;
}
