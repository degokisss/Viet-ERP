package com.vieterp.otb.budget.exception;

public class BudgetNotFoundException extends RuntimeException {
    public BudgetNotFoundException(String id) {
        super("Budget not found: " + id);
    }
}
