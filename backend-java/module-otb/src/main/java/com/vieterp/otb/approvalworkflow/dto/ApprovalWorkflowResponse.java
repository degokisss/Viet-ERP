package com.vieterp.otb.approvalworkflow.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Builder
public record ApprovalWorkflowResponse(
    Long id,
    Long groupBrandId,
    String groupBrandName,
    String workflowName,
    Instant createdAt,
    Instant updatedAt,
    CreatorSummary creator,
    List<ApprovalWorkflowLevelResponse> levels
) {
    @Builder
    public record CreatorSummary(
        Long id,
        String name,
        String email
    ) {}

    @Builder
    public record ApprovalWorkflowLevelResponse(
        Long id,
        Integer levelOrder,
        String levelName,
        Long approverUserId,
        String approverUserName,
        Boolean isRequired,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
