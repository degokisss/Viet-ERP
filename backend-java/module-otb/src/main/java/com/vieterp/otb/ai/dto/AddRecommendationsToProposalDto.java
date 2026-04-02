package com.vieterp.otb.ai.dto;

import lombok.*;
import java.util.List;

@Data
public class AddRecommendationsToProposalDto {
    private List<Long> productIds;
    private Long headerId;
}
