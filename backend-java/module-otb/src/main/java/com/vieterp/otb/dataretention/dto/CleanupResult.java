package com.vieterp.otb.dataretention.dto;

import java.time.Instant;

public record CleanupResult(
    long auditLogsDeleted,
    long archivedBudgetsDeleted,
    long importSessionsDeleted,
    long importRecordsDeleted,
    Instant executedAt,
    long durationMs
) {
}
