package com.receiptapp.expenses.expense;

import com.receiptapp.expenses.common.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseDetails(
        String supplierName,
        LocalDate expenseDate,
        CurrencyCode currency,
        BigDecimal amount,
        PaidWith paidWith,
        PaymentMethod paymentMethod,
        String fiscalInvoiceNumber) {
}