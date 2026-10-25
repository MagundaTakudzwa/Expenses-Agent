package com.receiptapp.expenses.expense;

public enum ExpenseStatus {
    DRAFT, // the worker is still adding receipts; managers can't see it yet
    SUBMITTED, // sent to a manager for review
    VERIFIED, // checked: genuine and valid
    QUERIED, // the manager needs more information from the worker
    REJECTED, // the company won't pay for it
    REIMBURSED // the worker has been paid back
}