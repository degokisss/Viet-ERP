package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Builder
public record BulkAddProductsRequest(
    @NotEmpty(message = "products required")
    List<CreateSKUProposalRequest> products
) {}
