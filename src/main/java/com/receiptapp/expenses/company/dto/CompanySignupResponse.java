package com.receiptapp.expenses.company.dto;

import java.time.Instant;

/**
 * Signing up also logs the new admin in, so the app can go straight to the
 * dashboard.
 */
public record CompanySignupResponse(Long companyId, String companyName, Long adminUserId,
        String accessToken, String tokenType, Instant expiresAt) {
}