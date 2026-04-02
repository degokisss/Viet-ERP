package com.vieterp.otb.notification.dto;

import java.util.List;

public record NotificationResponse(
    List<NotificationItem> items,
    Integer total,
    Integer pendingCount
) {
}
