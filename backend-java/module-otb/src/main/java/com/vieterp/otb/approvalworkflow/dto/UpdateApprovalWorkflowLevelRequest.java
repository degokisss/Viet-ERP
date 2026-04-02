package com.vieterp.otb.approvalworkflow.dto;

import lombok.*;

@Builder
public record UpdateApprovalWorkflowLevelRequest(
    String levelName,
    String approverUserId,
    Boolean isRequired
) {}
