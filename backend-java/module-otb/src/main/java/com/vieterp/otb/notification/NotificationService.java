package com.vieterp.otb.notification;

import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import com.vieterp.otb.budget.repository.BudgetRepository;
import com.vieterp.otb.notification.dto.NotificationItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final AuditLogRepository auditLogRepository;
    private final BudgetRepository budgetRepository;
    private final PlanningHeaderRepository planningHeaderRepository;
    private final SKUProposalHeaderRepository skuProposalHeaderRepository;

    private static final List<String> APPROVAL_ACTIONS = List.of("APPROVED", "REJECTED");

    public List<NotificationItem> getNotifications(Long userId, Integer limit) {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        List<AuditLog> approvalLogs = auditLogRepository.findByCreatedAtGreaterThanEqualAndActionIn(
            sevenDaysAgo, APPROVAL_ACTIONS);

        List<NotificationItem> items = new ArrayList<>();

        for (AuditLog log : approvalLogs) {
            if (!isUserEntityCreator(userId, log)) {
                continue;
            }

            NotificationItem item = buildNotificationItem(log);
            if (item != null) {
                items.add(item);
            }
        }

        int pendingCount = countPendingItems(userId);
        if (pendingCount > 0) {
            items.add(0, NotificationItem.builder()
                .id(0L)
                .type("pending_action")
                .entityType("pending")
                .entityId(0L)
                .title("Pending Approvals")
                .message("You have " + pendingCount + " items awaiting approval")
                .severity("warning")
                .createdAt(Instant.now())
                .read(false)
                .build());
        }

        if (limit != null && limit > 0 && items.size() > limit) {
            return items.subList(0, limit);
        }

        return items;
    }

    private boolean isUserEntityCreator(Long userId, AuditLog log) {
        String entityType = log.getEntityType();
        String entityIdStr = log.getEntityId();

        if (entityIdStr == null) {
            return false;
        }

        Long entityId;
        try {
            entityId = Long.parseLong(entityIdStr);
        } catch (NumberFormatException e) {
            return false;
        }

        return switch (entityType) {
            case "Budget" -> budgetRepository.findById(entityId)
                .map(b -> b.getCreator() != null && userId.equals(b.getCreator().getId()))
                .orElse(false);
            case "PlanningHeader" -> planningHeaderRepository.findByIdWithHeader(entityId)
                .map(p -> p.getCreator() != null && userId.equals(p.getCreator().getId()))
                .orElse(false);
            case "SKUProposalHeader" -> skuProposalHeaderRepository.findById(entityId)
                .map(p -> p.getCreator() != null && userId.equals(p.getCreator().getId()))
                .orElse(false);
            default -> false;
        };
    }

    private NotificationItem buildNotificationItem(AuditLog log) {
        String action = log.getAction();
        String entityType = log.getEntityType();
        String entityIdStr = log.getEntityId();

        Long entityId = null;
        try {
            entityId = entityIdStr != null ? Long.parseLong(entityIdStr) : null;
        } catch (NumberFormatException ignored) {
        }

        String type = "approval";
        String title = action.equals("APPROVED") ? "Approved" : "Rejected";
        String severity = action.equals("APPROVED") ? "success" : "error";
        String message = String.format("Your %s has been %s",
            formatEntityType(entityType), action.toLowerCase());

        return NotificationItem.builder()
            .id(log.getId())
            .type(type)
            .entityType(entityType)
            .entityId(entityId)
            .title(title)
            .message(message)
            .severity(severity)
            .createdAt(log.getCreatedAt())
            .read(false)
            .build();
    }

    private String formatEntityType(String entityType) {
        if (entityType == null) {
            return "item";
        }
        return switch (entityType) {
            case "Budget" -> "budget";
            case "PlanningHeader" -> "planning";
            case "SKUProposalHeader" -> "proposal";
            default -> entityType.toLowerCase();
        };
    }

    private int countPendingItems(Long userId) {
        int count = 0;

        Specification<Budget> budgetSpec = (root, cq, cb) -> cb.and(
            cb.equal(root.get("creator").get("id"), userId),
            root.get("status").in("SUBMITTED", "LEVEL1_APPROVED")
        );
        count += budgetRepository.count(budgetSpec);

        Specification<PlanningHeader> planningSpec = (root, cq, cb) -> cb.and(
            cb.equal(root.get("creator").get("id"), userId),
            root.get("status").in("SUBMITTED", "LEVEL1_APPROVED")
        );
        count += planningHeaderRepository.count(planningSpec);

        Specification<SKUProposalHeader> proposalSpec = (root, cq, cb) -> cb.and(
            cb.equal(root.get("creator").get("id"), userId),
            root.get("status").in("SUBMITTED", "LEVEL1_APPROVED")
        );
        count += skuProposalHeaderRepository.count(proposalSpec);

        return count;
    }
}
