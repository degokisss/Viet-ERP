package com.vieterp.otb.proposal.exception;

public class ProposalNotFoundException extends RuntimeException {
    public ProposalNotFoundException(String id) {
        super("Proposal not found: " + id);
    }
}
