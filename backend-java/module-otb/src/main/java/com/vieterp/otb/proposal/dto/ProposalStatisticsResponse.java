package com.vieterp.otb.proposal.dto;

import lombok.*;
import java.util.Map;

@Builder
public record ProposalStatisticsResponse(
    long totalProposals,
    Map<String, Long> byStatus
) {}
