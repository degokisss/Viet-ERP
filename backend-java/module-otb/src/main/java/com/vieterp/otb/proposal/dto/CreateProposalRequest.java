package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Builder
public record CreateProposalRequest(
    String allocateHeaderId,

    Boolean isFinalVersion
) {}
