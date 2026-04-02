package com.vieterp.otb.ai.dto;

import lombok.*;

@Data
@Builder
public class AlertDto {
    private Long id;
    private Long budgetId;
    private String budgetName;
    private String alertType;
    private String severity;
    private String title;
    private String message;
    private Boolean isRead;
    private Boolean isDismissed;
}
