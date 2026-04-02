package com.vieterp.otb.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
public record CreateTicketRequest(
    @NotBlank(message = "budgetId is required")
    String budgetId,

    String seasonGroupId,

    String seasonId,

    @NotNull(message = "approvalWorkflowId is required")
    String approvalWorkflowId
) {}
