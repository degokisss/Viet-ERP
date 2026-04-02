package com.vieterp.otb.ai.dto;

import lombok.*;

@Data
public class GetAlertsQueryDto {
    private Long budgetId;
    private Boolean unreadOnly;
}
