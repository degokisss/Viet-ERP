package com.vieterp.otb.ticket.dto;

import lombok.*;
import java.util.List;

@Builder
public record TicketValidationResponse(
    boolean ready,
    List<ValidationStep> steps
) {
    @Builder
    public record ValidationStep(
        String name,
        boolean passed,
        String message,
        List<String> details
    ) {}
}
