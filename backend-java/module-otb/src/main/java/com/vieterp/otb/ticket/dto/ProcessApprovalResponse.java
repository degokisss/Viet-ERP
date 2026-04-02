package com.vieterp.otb.ticket.dto;

import lombok.*;

@Builder
public record ProcessApprovalResponse(
    TicketApprovalLogResponse log,
    String newStatus
) {
    @Builder
    public record TicketApprovalLogResponse(
        Long id,
        Long approvalWorkflowLevelId,
        Long approverUserId,
        Boolean isApproved,
        String comment,
        java.time.Instant approvedAt
    ) {}
}
