package com.vieterp.otb.ticket.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Builder
public record TicketResponse(
    Long id,
    Long budgetId,
    String budgetName,
    Long seasonGroupId,
    String seasonGroupName,
    Long seasonId,
    String seasonName,
    String status,
    Instant createdAt,
    Instant updatedAt,
    CreatorSummary creator,
    List<TicketApprovalLogResponse> approvalHistory
) {
    @Builder
    public record CreatorSummary(
        Long id,
        String name,
        String email
    ) {}

    @Builder
    public record TicketApprovalLogResponse(
        Long id,
        Long approvalWorkflowLevelId,
        String levelName,
        Long approverUserId,
        String approverUserName,
        Boolean isApproved,
        String comment,
        Instant approvedAt
    ) {}
}
