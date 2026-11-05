package com.receiptapp.expenses.security.dto;

import com.receiptapp.expenses.user.Role;

public record MeResponse(Long userId, String fullName, String email, Role role,
        Long companyId, String companyName) {
}