package com.vieterp.otb.service;

import com.vieterp.otb.budget.repository.BudgetRepository;
import com.vieterp.otb.domain.AuditLog;
import com.vieterp.otb.domain.Budget;
import com.vieterp.otb.domain.PlanningHeader;
import com.vieterp.otb.domain.SKUProposalHeader;
import com.vieterp.otb.domain.User;
import com.vieterp.otb.domain.repository.AuditLogRepository;
import com.vieterp.otb.domain.repository.PlanningHeaderRepository;
import com.vieterp.otb.domain.repository.SKUProposalHeaderRepository;
import com.vieterp.otb.notification.NotificationService;
import com.vieterp.otb.notification.dto.NotificationItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private PlanningHeaderRepository planningHeaderRepository;
    @Mock private SKUProposalHeaderRepository skuProposalHeaderRepository;

    private NotificationService notificationService;

    private User testUser;
    private AuditLog testAuditLog;
    private Budget testBudget;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            auditLogRepository,
            budgetRepository,
            planningHeaderRepository,
            skuProposalHeaderRepository
        );

        testUser = User.builder()
            .id(1L)
            .name("Test User")
            .email("test@vieterp.com")
            .build();

        testBudget = Budget.builder()
            .id(10L)
            .name("Q1 Budget")
            .amount(new BigDecimal("1000000"))
            .status("APPROVED")
            .creator(testUser)
            .createdAt(Instant.now())
            .build();

        testAuditLog = AuditLog.builder()
            .id(100L)
            .userId(1L)
            .action("APPROVED")
            .entityType("Budget")
            .entityId("10")
            .createdAt(Instant.now())
            .build();
    }

    // ─── GET NOTIFICATIONS ─────────────────────────────────────────────────────

    @Test
    void getNotifications_returnsItemsFromAuditLogs() {
        when(auditLogRepository.findByCreatedAtGreaterThanEqualAndActionIn(
            any(Instant.class),
            eq(List.of("APPROVED", "REJECTED"))
        )).thenReturn(List.of(testAuditLog));

        when(budgetRepository.findById(10L)).thenReturn(java.util.Optional.of(testBudget));

        when(budgetRepository.count(any(Specification.class))).thenReturn(0L);
        when(planningHeaderRepository.count(any(Specification.class))).thenReturn(0L);
        when(skuProposalHeaderRepository.count(any(Specification.class))).thenReturn(0L);

        List<NotificationItem> result = notificationService.getNotifications(1L, null);

        assertFalse(result.isEmpty());
        assertEquals("Budget", result.get(0).entityType());
        verify(auditLogRepository).findByCreatedAtGreaterThanEqualAndActionIn(
            any(Instant.class),
            eq(List.of("APPROVED", "REJECTED"))
        );
    }

    @Test
    void getNotifications_withNoAuditLogs_returnsEmptyOrPendingOnly() {
        when(auditLogRepository.findByCreatedAtGreaterThanEqualAndActionIn(
            any(Instant.class),
            eq(List.of("APPROVED", "REJECTED"))
        )).thenReturn(List.of());

        when(budgetRepository.count(any(Specification.class))).thenReturn(0L);
        when(planningHeaderRepository.count(any(Specification.class))).thenReturn(0L);
        when(skuProposalHeaderRepository.count(any(Specification.class))).thenReturn(0L);

        List<NotificationItem> result = notificationService.getNotifications(1L, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getNotifications_countsPendingCorrectly() {
        when(auditLogRepository.findByCreatedAtGreaterThanEqualAndActionIn(
            any(Instant.class),
            eq(List.of("APPROVED", "REJECTED"))
        )).thenReturn(List.of());

        when(budgetRepository.count(any(Specification.class))).thenReturn(3L);
        when(planningHeaderRepository.count(any(Specification.class))).thenReturn(2L);
        when(skuProposalHeaderRepository.count(any(Specification.class))).thenReturn(1L);

        List<NotificationItem> result = notificationService.getNotifications(1L, null);

        assertEquals(1, result.size());
        assertEquals("pending_action", result.get(0).type());
        assertEquals("You have 6 items awaiting approval", result.get(0).message());
    }

    @Test
    void getNotifications_rejectedAction_buildsCorrectNotification() {
        AuditLog rejectedLog = AuditLog.builder()
            .id(101L)
            .userId(1L)
            .action("REJECTED")
            .entityType("Budget")
            .entityId("10")
            .createdAt(Instant.now())
            .build();

        when(auditLogRepository.findByCreatedAtGreaterThanEqualAndActionIn(
            any(Instant.class),
            eq(List.of("APPROVED", "REJECTED"))
        )).thenReturn(List.of(rejectedLog));

        when(budgetRepository.findById(10L)).thenReturn(java.util.Optional.of(testBudget));

        when(budgetRepository.count(any(Specification.class))).thenReturn(0L);
        when(planningHeaderRepository.count(any(Specification.class))).thenReturn(0L);
        when(skuProposalHeaderRepository.count(any(Specification.class))).thenReturn(0L);

        List<NotificationItem> result = notificationService.getNotifications(1L, null);

        assertEquals("error", result.get(0).severity());
        assertEquals("Rejected", result.get(0).title());
        assertEquals("Your budget has been rejected", result.get(0).message());
    }

    @Test
    void getNotifications_withLimit_returnsLimitedResults() {
        when(auditLogRepository.findByCreatedAtGreaterThanEqualAndActionIn(
            any(Instant.class),
            eq(List.of("APPROVED", "REJECTED"))
        )).thenReturn(List.of(testAuditLog));

        when(budgetRepository.findById(10L)).thenReturn(java.util.Optional.of(testBudget));

        when(budgetRepository.count(any(Specification.class))).thenReturn(0L);
        when(planningHeaderRepository.count(any(Specification.class))).thenReturn(0L);
        when(skuProposalHeaderRepository.count(any(Specification.class))).thenReturn(0L);

        List<NotificationItem> result = notificationService.getNotifications(1L, 1);

        assertEquals(1, result.size());
    }

    @Test
    void getNotifications_nonCreatorNotIncluded() {
        User otherUser = User.builder().id(99L).name("Other").email("other@vieterp.com").build();
        Budget otherBudget = Budget.builder()
            .id(20L)
            .name("Other Budget")
            .creator(otherUser)
            .createdAt(Instant.now())
            .build();

        AuditLog otherLog = AuditLog.builder()
            .id(102L)
            .userId(99L)
            .action("APPROVED")
            .entityType("Budget")
            .entityId("20")
            .createdAt(Instant.now())
            .build();

        when(auditLogRepository.findByCreatedAtGreaterThanEqualAndActionIn(
            any(Instant.class),
            eq(List.of("APPROVED", "REJECTED"))
        )).thenReturn(List.of(otherLog));

        when(budgetRepository.findById(20L)).thenReturn(java.util.Optional.of(otherBudget));

        when(budgetRepository.count(any(Specification.class))).thenReturn(0L);
        when(planningHeaderRepository.count(any(Specification.class))).thenReturn(0L);
        when(skuProposalHeaderRepository.count(any(Specification.class))).thenReturn(0L);

        List<NotificationItem> result = notificationService.getNotifications(1L, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getNotifications_planningHeaderApproval_buildsCorrectMessage() {
        PlanningHeader planningHeader = PlanningHeader.builder()
            .id(30L)
            .creator(testUser)
            .status("APPROVED")
            .createdAt(Instant.now())
            .build();

        AuditLog planningLog = AuditLog.builder()
            .id(103L)
            .userId(1L)
            .action("APPROVED")
            .entityType("PlanningHeader")
            .entityId("30")
            .createdAt(Instant.now())
            .build();

        when(auditLogRepository.findByCreatedAtGreaterThanEqualAndActionIn(
            any(Instant.class),
            eq(List.of("APPROVED", "REJECTED"))
        )).thenReturn(List.of(planningLog));

        when(planningHeaderRepository.findByIdWithHeader(30L))
            .thenReturn(java.util.Optional.of(planningHeader));

        when(budgetRepository.count(any(Specification.class))).thenReturn(0L);
        when(planningHeaderRepository.count(any(Specification.class))).thenReturn(0L);
        when(skuProposalHeaderRepository.count(any(Specification.class))).thenReturn(0L);

        List<NotificationItem> result = notificationService.getNotifications(1L, null);

        assertEquals("planning", result.get(0).message().split(" ")[1].replace(".", ""));
        assertEquals("Approved", result.get(0).title());
    }

    @Test
    void getNotifications_skuProposalApproval_buildsCorrectMessage() {
        SKUProposalHeader proposalHeader = SKUProposalHeader.builder()
            .id(40L)
            .creator(testUser)
            .status("APPROVED")
            .createdAt(Instant.now())
            .build();

        AuditLog proposalLog = AuditLog.builder()
            .id(104L)
            .userId(1L)
            .action("APPROVED")
            .entityType("SKUProposalHeader")
            .entityId("40")
            .createdAt(Instant.now())
            .build();

        when(auditLogRepository.findByCreatedAtGreaterThanEqualAndActionIn(
            any(Instant.class),
            eq(List.of("APPROVED", "REJECTED"))
        )).thenReturn(List.of(proposalLog));

        when(skuProposalHeaderRepository.findById(40L))
            .thenReturn(java.util.Optional.of(proposalHeader));

        when(budgetRepository.count(any(Specification.class))).thenReturn(0L);
        when(planningHeaderRepository.count(any(Specification.class))).thenReturn(0L);
        when(skuProposalHeaderRepository.count(any(Specification.class))).thenReturn(0L);

        List<NotificationItem> result = notificationService.getNotifications(1L, null);

        assertEquals("proposal", result.get(0).message().split(" ")[1].replace(".", ""));
        assertEquals("Approved", result.get(0).title());
    }
}
