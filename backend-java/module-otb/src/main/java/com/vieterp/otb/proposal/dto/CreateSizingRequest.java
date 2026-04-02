package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Builder
public record CreateSizingRequest(
    @NotEmpty(message = "sizings required")
    List<ProposalSizingDto> sizings
) {}
