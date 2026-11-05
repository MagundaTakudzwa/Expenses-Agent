package com.receiptapp.expenses.company.dto;

import com.receiptapp.expenses.common.CurrencyCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A new business signs up. The person signing up becomes its first admin. */
public record CompanySignupRequest(
        @NotBlank(message = "is required") @Size(max = 150, message = "is too long") String companyName,

        // Optional: defaults to USD.
        CurrencyCode baseCurrency,

        @NotBlank(message = "is required") @Size(max = 100, message = "is too long") String adminFullName,

        @NotBlank(message = "is required") @Email(message = "must be a valid email") @Size(max = 150, message = "is too long") String adminEmail,

        @Pattern(regexp = "^\\+?[0-9 ]{9,16}$", message = "must be a valid phone number, e.g. +263 77 123 4567") String adminPhone,

        @NotBlank(message = "is required") @Size(min = 10, max = 64, message = "must be between 10 and 64 characters") String password) {
}