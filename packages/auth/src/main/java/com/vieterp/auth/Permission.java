package com.vieterp.auth;

/**
 * VietERP permission constants.
 * Must stay in sync with TypeScript Permission enum in packages/auth/src/permission.ts
 */
public enum Permission {
    // HRM
    HRM_EMPLOYEE_READ,
    HRM_EMPLOYEE_WRITE,
    HRM_EMPLOYEE_DELETE,
    HRM_DEPARTMENT_READ,
    HRM_DEPARTMENT_WRITE,
    // CRM
    CRM_CONTACT_READ,
    CRM_CONTACT_WRITE,
    CRM_CONTACT_DELETE,
    // Accounting
    ACCOUNTING_INVOICE_READ,
    ACCOUNTING_INVOICE_WRITE,
    ACCOUNTING_INVOICE_DELETE,
    // MRP
    MRP_PLAN_READ,
    MRP_PLAN_WRITE,
    MRP_BOM_READ,
    MRP_BOM_WRITE,
    // TPM
    TPM_PROMOTION_READ,
    TPM_PROMOTION_WRITE,
    // Common
    ADMIN_ALL,
    AUDIT_READ;
}
