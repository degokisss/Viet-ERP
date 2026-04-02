package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
public class FullSizingDto {
    @NotBlank(message = "skuProposalId is required")
    private String skuProposalId;

    @NotBlank(message = "subcategorySizeId is required")
    private String subcategorySizeId;

    @DecimalMin("0")
    private BigDecimal actualSalesmixPct;

    @DecimalMin("0")
    private BigDecimal actualStPct;

    @Min(0)
    private Integer proposalQuantity;
}
