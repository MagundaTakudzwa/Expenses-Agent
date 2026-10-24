package com.receiptapp.expenses.expense;

public enum ExtractionStatus {
    NOT_APPLICABLE, // mileage and subsistence claims have no receipt to read
    PENDING, // waiting to be read
    EXTRACTED, // read with good confidence
    LOW_CONFIDENCE, // read, but someone should check it
    UNREADABLE // couldn't be read, so the manager reviews the photo
}