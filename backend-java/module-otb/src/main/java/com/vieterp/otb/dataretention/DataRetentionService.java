package com.vieterp.otb.dataretention;

import com.vieterp.otb.budget.repository.BudgetRepository;
import com.vieterp.otb.dataretention.dto.CleanupResult;
import com.vieterp.otb.dataretention.dto.RetentionPolicyResponse;
import com.vieterp.otb.domain.Budget;
import com.vieterp.otb.domain.repository.AuditLogRepository;
import com.vieterp.otb.domain.repository.ImportSessionRepository;
import com.vieterp.otb.domain.repository.ImportedRecordRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DataRetentionService {

    private final AuditLogRepository auditLogRepository;
    private final BudgetRepository budgetRepository;
    private final ImportSessionRepository importSessionRepository;
    private final ImportedRecordRepository importedRecordRepository;

    public DataRetentionService(
            AuditLogRepository auditLogRepository,
            BudgetRepository budgetRepository,
            @Qualifier("dataRetentionImportSessionRepository") ImportSessionRepository importSessionRepository,
            @Qualifier("dataRetentionImportedRecordRepository") ImportedRecordRepository importedRecordRepository) {
        this.auditLogRepository = auditLogRepository;
        this.budgetRepository = budgetRepository;
        this.importSessionRepository = importSessionRepository;
        this.importedRecordRepository = importedRecordRepository;
    }

    public RetentionPolicyResponse getRetentionPolicy() {
        return new RetentionPolicyResponse(
            DataRetentionConstants.AUDIT_LOG_RETENTION_DAYS,
            DataRetentionConstants.ARCHIVED_BUDGET_RETENTION_DAYS,
            DataRetentionConstants.IMPORT_SESSION_RETENTION_DAYS
        );
    }

    @Transactional
    public CleanupResult cleanup() {
        Instant start = Instant.now();

        long auditLogsDeleted = cleanupAuditLogs();
        long archivedBudgetsDeleted = cleanupArchivedBudgets();
        long importRecordsDeleted = cleanupImportRecords();
        long importSessionsDeleted = cleanupImportSessions();

        Instant end = Instant.now();
        long durationMs = ChronoUnit.MILLIS.between(start, end);

        return new CleanupResult(
            auditLogsDeleted,
            archivedBudgetsDeleted,
            importSessionsDeleted,
            importRecordsDeleted,
            end,
            durationMs
        );
    }

    private long cleanupAuditLogs() {
        Instant cutoff = Instant.now().minus(DataRetentionConstants.AUDIT_LOG_RETENTION_DAYS, ChronoUnit.DAYS);
        long count = countAuditLogsBefore(cutoff);
        auditLogRepository.deleteByCreatedAtLessThan(cutoff);
        return count;
    }

    private long countAuditLogsBefore(Instant cutoff) {
        Specification<com.vieterp.otb.domain.AuditLog> spec = (root, cq, cb) ->
            cb.lessThan(root.get("createdAt"), cutoff);
        return auditLogRepository.count(spec);
    }

    private long cleanupArchivedBudgets() {
        Instant cutoff = Instant.now().minus(DataRetentionConstants.ARCHIVED_BUDGET_RETENTION_DAYS, ChronoUnit.DAYS);
        long count = countArchivedBudgetsBefore(cutoff);

        Specification<Budget> spec = (root, cq, cb) -> cb.and(
            cb.equal(root.get("status"), "ARCHIVED"),
            cb.lessThan(root.get("updatedAt"), cutoff)
        );
        List<Budget> archived = budgetRepository.findAll(spec);
        if (!archived.isEmpty()) {
            budgetRepository.deleteAll(archived);
        }

        return count;
    }

    private long countArchivedBudgetsBefore(Instant cutoff) {
        Specification<Budget> spec = (root, cq, cb) -> cb.and(
            cb.equal(root.get("status"), "ARCHIVED"),
            cb.lessThan(root.get("updatedAt"), cutoff)
        );
        return budgetRepository.count(spec);
    }

    private long cleanupImportSessions() {
        Instant cutoff = Instant.now().minus(DataRetentionConstants.IMPORT_SESSION_RETENTION_DAYS, ChronoUnit.DAYS);
        long count = countImportSessionsBefore(cutoff);
        importSessionRepository.deleteByCreatedAtLessThan(cutoff);
        return count;
    }

    private long countImportSessionsBefore(Instant cutoff) {
        Specification<com.vieterp.otb.domain.ImportSession> spec = (root, cq, cb) ->
            cb.lessThan(root.get("createdAt"), cutoff);
        return importSessionRepository.count(spec);
    }

    private long cleanupImportRecords() {
        Instant cutoff = Instant.now().minus(DataRetentionConstants.IMPORT_SESSION_RETENTION_DAYS, ChronoUnit.DAYS);
        long count = countImportRecordsBefore(cutoff);
        importedRecordRepository.deleteByCreatedAtLessThan(cutoff);
        return count;
    }

    private long countImportRecordsBefore(Instant cutoff) {
        Specification<com.vieterp.otb.domain.ImportedRecord> spec = (root, cq, cb) ->
            cb.lessThan(root.get("createdAt"), cutoff);
        return importedRecordRepository.count(spec);
    }
}
