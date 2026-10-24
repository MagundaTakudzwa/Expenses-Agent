package com.receiptapp.expenses.expense;

public enum ExpenseStatus {
    SUBMITTED, // waiting for a manager
    VERIFIED, // checked: genuine and valid
    QUERIED, // the manager needs more information from the worker
    REJECTED, // the company won't pay for it
    REIMBURSED // the worker has been paid back
}