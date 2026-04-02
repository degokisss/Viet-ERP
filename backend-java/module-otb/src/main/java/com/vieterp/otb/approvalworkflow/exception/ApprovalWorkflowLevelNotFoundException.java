package com.vieterp.otb.approvalworkflow.exception;

public class ApprovalWorkflowLevelNotFoundException extends RuntimeException {
    public ApprovalWorkflowLevelNotFoundException(String id) {
        super("ApprovalWorkflowLevel not found with id: " + id);
    }
}
