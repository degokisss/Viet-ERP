package com.vieterp.otb.notification.dto;

import lombok.*;

import java.time.Instant;

@Builder
public record NotificationItem(
    Long id,
    String type,
    String entityType,
    Long entityId,
    String title,
    String message,
    String severity,
    Instant createdAt,
    Boolean read
) {
    public enum Type {
        approval,
        status_change,
        pending_action
    }

    public enum Severity {
        info,
        warning,
        success,
        error
    }
}
