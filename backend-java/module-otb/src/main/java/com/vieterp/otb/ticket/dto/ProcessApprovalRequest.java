package com.vieterp.otb.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
public record ProcessApprovalRequest(
    @NotBlank(message = "approvalWorkflowLevelId is required")
    String approvalWorkflowLevelId,

    @NotNull(message = "isApproved is required")
    Boolean isApproved,

    String comment
) {}
