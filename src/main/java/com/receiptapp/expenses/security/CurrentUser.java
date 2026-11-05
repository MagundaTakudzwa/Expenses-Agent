package com.receiptapp.expenses.security;

import com.receiptapp.expenses.user.Role;

/**
 * Who is making this request. Built fresh from the database on every request.
 */
public record CurrentUser(Long id, Long companyId, Role role, String fullName) {

    public boolean hasAnyRole(Role... roles) {
        for (Role r : roles) {
            if (r == role) {
                return true;
            }
        }
        return false;
    }
}