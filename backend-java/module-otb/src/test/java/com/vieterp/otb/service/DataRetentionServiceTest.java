package com.vieterp.otb.service;

import com.vieterp.otb.budget.repository.BudgetRepository;
import com.vieterp.otb.dataretention.DataRetentionConstants;
import com.vieterp.otb.dataretention.DataRetentionService;
import com.vieterp.otb.dataretention.dto.CleanupResult;
import com.vieterp.otb.dataretention.dto.RetentionPolicyResponse;
import com.vieterp.otb.domain.Budget;
import com.vieterp.otb.domain.repository.AuditLogRepository;
import com.vieterp.otb.domain.repository.ImportSessionRepository;
import com.vieterp.otb.domain.repository.ImportedRecordRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataRetentionServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private ImportSessionRepository importSessionRepository;
    @Mock private ImportedRecordRepository importedRecordRepository;

    private DataRetentionService dataRetentionService;

    @BeforeEach
    void setUp() {
        dataRetentionService = new DataRetentionService(
            auditLogRepository,
            budgetRepository,
            importSessionRepository,
            importedRecordRepository
        );
    }

    // ─── GET RETENTION POLICY ──────────────────────────────────────────────────

    @Test
    void getRetentionPolicy_returnsConstants() {
        RetentionPolicyResponse response = dataRetentionService.getRetentionPolicy();

        assertEquals(DataRetentionConstants.AUDIT_LOG_RETENTION_DAYS, response.auditLogRetentionDays());
        assertEquals(DataRetentionConstants.ARCHIVED_BUDGET_RETENTION_DAYS, response.archivedBudgetRetentionDays());
        assertEquals(DataRetentionConstants.IMPORT_SESSION_RETENTION_DAYS, response.importSessionRetentionDays());
    }

    // ─── CLEANUP ──────────────────────────────────────────────────────────────

    @Test
    void cleanup_deletesOldRecordsAndReturnsCounts() {
        Instant now = Instant.now();
        Instant auditCutoff = now.minus(DataRetentionConstants.AUDIT_LOG_RETENTION_DAYS, ChronoUnit.DAYS);
        Instant budgetCutoff = now.minus(DataRetentionConstants.ARCHIVED_BUDGET_RETENTION_DAYS, ChronoUnit.DAYS);
        Instant importCutoff = now.minus(DataRetentionConstants.IMPORT_SESSION_RETENTION_DAYS, ChronoUnit.DAYS);

        when(auditLogRepository.count(any(Specification.class))).thenReturn(5L);
        doNothing().when(auditLogRepository).deleteByCreatedAtLessThan(any(Instant.class));

        Budget archivedBudget = Budget.builder()
            .id(1L)
            .name("Old Budget")
            .status("ARCHIVED")
            .updatedAt(auditCutoff.minus(1, ChronoUnit.DAYS))
            .build();
        when(budgetRepository.count(any(Specification.class))).thenReturn(1L);
        when(budgetRepository.findAll(any(Specification.class))).thenReturn(List.of(archivedBudget));
        doNothing().when(budgetRepository).deleteAll(anyList());

        when(importSessionRepository.count(any(Specification.class))).thenReturn(3L);
        doNothing().when(importSessionRepository).deleteByCreatedAtLessThan(any(Instant.class));

        when(importedRecordRepository.count(any(Specification.class))).thenReturn(10L);
        doNothing().when(importedRecordRepository).deleteByCreatedAtLessThan(any(Instant.class));

        CleanupResult result = dataRetentionService.cleanup();

        assertEquals(5L, result.auditLogsDeleted());
        assertEquals(1L, result.archivedBudgetsDeleted());
        assertEquals(3L, result.importSessionsDeleted());
        assertEquals(10L, result.importRecordsDeleted());
        assertTrue(result.durationMs() >= 0);
        assertNotNull(result.executedAt());
    }

    @Test
    void cleanup_withNoOldRecords_returnsZeroCounts() {
        when(auditLogRepository.count(any(Specification.class))).thenReturn(0L);
        doNothing().when(auditLogRepository).deleteByCreatedAtLessThan(any(Instant.class));

        when(budgetRepository.count(any(Specification.class))).thenReturn(0L);
        when(budgetRepository.findAll(any(Specification.class))).thenReturn(List.of());

        when(importSessionRepository.count(any(Specification.class))).thenReturn(0L);
        doNothing().when(importSessionRepository).deleteByCreatedAtLessThan(any(Instant.class));

        when(importedRecordRepository.count(any(Specification.class))).thenReturn(0L);
        doNothing().when(importedRecordRepository).deleteByCreatedAtLessThan(any(Instant.class));

        CleanupResult result = dataRetentionService.cleanup();

        assertEquals(0L, result.auditLogsDeleted());
        assertEquals(0L, result.archivedBudgetsDeleted());
        assertEquals(0L, result.importSessionsDeleted());
        assertEquals(0L, result.importRecordsDeleted());
    }

    @Test
    void cleanup_archivedBudgetsOnlyDeletesWhenListNotEmpty() {
        when(auditLogRepository.count(any(Specification.class))).thenReturn(0L);
        doNothing().when(auditLogRepository).deleteByCreatedAtLessThan(any(Instant.class));

        when(budgetRepository.count(any(Specification.class))).thenReturn(0L);
        when(budgetRepository.findAll(any(Specification.class))).thenReturn(List.of());

        when(importSessionRepository.count(any(Specification.class))).thenReturn(0L);
        doNothing().when(importSessionRepository).deleteByCreatedAtLessThan(any(Instant.class));

        when(importedRecordRepository.count(any(Specification.class))).thenReturn(0L);
        doNothing().when(importedRecordRepository).deleteByCreatedAtLessThan(any(Instant.class));

        CleanupResult result = dataRetentionService.cleanup();

        verify(budgetRepository, never()).deleteAll(anyList());
    }

    @Test
    void cleanup_deletesMultipleArchivedBudgets() {
        Budget budget1 = Budget.builder()
            .id(1L)
            .name("Archived Budget 1")
            .status("ARCHIVED")
            .updatedAt(Instant.now().minus(1000, ChronoUnit.DAYS))
            .build();
        Budget budget2 = Budget.builder()
            .id(2L)
            .name("Archived Budget 2")
            .status("ARCHIVED")
            .updatedAt(Instant.now().minus(800, ChronoUnit.DAYS))
            .build();

        when(auditLogRepository.count(any(Specification.class))).thenReturn(0L);
        doNothing().when(auditLogRepository).deleteByCreatedAtLessThan(any(Instant.class));

        when(budgetRepository.count(any(Specification.class))).thenReturn(2L);
        when(budgetRepository.findAll(any(Specification.class))).thenReturn(List.of(budget1, budget2));
        doNothing().when(budgetRepository).deleteAll(anyList());

        when(importSessionRepository.count(any(Specification.class))).thenReturn(0L);
        doNothing().when(importSessionRepository).deleteByCreatedAtLessThan(any(Instant.class));

        when(importedRecordRepository.count(any(Specification.class))).thenReturn(0L);
        doNothing().when(importedRecordRepository).deleteByCreatedAtLessThan(any(Instant.class));

        CleanupResult result = dataRetentionService.cleanup();

        assertEquals(2L, result.archivedBudgetsDeleted());
        verify(budgetRepository).deleteAll(List.of(budget1, budget2));
    }
}
