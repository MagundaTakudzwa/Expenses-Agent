package com.receiptapp.expenses.expense;

import com.receiptapp.expenses.common.CurrencyCode;

import java.math.BigDecimal;

/** One line of a finance total: currencies are never added together. */
public record CurrencyTotal(CurrencyCode currency, BigDecimal total, Long claimCount) {
}