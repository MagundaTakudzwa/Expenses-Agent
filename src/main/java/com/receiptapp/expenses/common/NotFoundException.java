package com.receiptapp.expenses.common;

/**
 * Becomes a 404. Also used when a record belongs to another company, so we
 * don't reveal it exists.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}