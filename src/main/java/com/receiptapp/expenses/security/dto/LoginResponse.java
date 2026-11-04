package com.receiptapp.expenses.security.dto;

import com.receiptapp.expenses.user.Role;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt,
        Long userId, String fullName, Role role) {
}