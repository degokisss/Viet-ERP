package com.vieterp.otb.approvalworkflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Builder
public record CreateApprovalWorkflowRequest(
    @NotBlank(message = "groupBrandId is required")
    String groupBrandId,

    @NotBlank(message = "workflowName is required")
    String workflowName,

    List<ApprovalWorkflowLevelRequest> levels
) {
    @Builder
    public record ApprovalWorkflowLevelRequest(
        String levelName,
        String approverUserId,
        Boolean isRequired
    ) {}
}
