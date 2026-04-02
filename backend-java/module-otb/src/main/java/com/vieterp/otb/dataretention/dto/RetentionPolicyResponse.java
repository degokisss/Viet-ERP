package com.vieterp.otb.dataretention.dto;

public record RetentionPolicyResponse(
    int auditLogRetentionDays,
    int archivedBudgetRetentionDays,
    int importSessionRetentionDays
) {
}
