package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Builder
public record SaveFullProposalRequest(
    List<CreateSKUProposalRequest> products,
    List<FullAllocationDto> allocations,
    List<FullSizingHeaderDto> sizingHeaders
) {}
