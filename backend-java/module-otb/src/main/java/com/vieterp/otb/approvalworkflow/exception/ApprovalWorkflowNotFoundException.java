package com.vieterp.otb.approvalworkflow.exception;

public class ApprovalWorkflowNotFoundException extends RuntimeException {
    public ApprovalWorkflowNotFoundException(String id) {
        super("ApprovalWorkflow not found with id: " + id);
    }
}
