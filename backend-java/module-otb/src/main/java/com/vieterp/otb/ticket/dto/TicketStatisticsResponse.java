package com.vieterp.otb.ticket.dto;

import lombok.*;
import java.util.Map;

@Builder
public record TicketStatisticsResponse(
    long totalTickets,
    Map<String, Long> byStatus,
    long pendingApprovals,
    long approvedTickets,
    long rejectedTickets
) {}
