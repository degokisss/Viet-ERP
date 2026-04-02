package com.vieterp.otb.notification;

import com.vieterp.otb.notification.dto.NotificationItem;
import com.vieterp.otb.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification management")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get notifications for a user")
    public ResponseEntity<NotificationResponse> getNotifications(
            @Parameter(description = "User ID") @RequestParam(name = "userId") Long userId,
            @Parameter(description = "Maximum number of notifications to return") @RequestParam(name = "limit", required = false) Integer limit) {
        List<NotificationItem> items = notificationService.getNotifications(userId, limit);
        return ResponseEntity.ok(new NotificationResponse(items, items.size(), 0));
    }
}
