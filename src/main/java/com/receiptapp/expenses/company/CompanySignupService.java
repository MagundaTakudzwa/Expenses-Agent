package com.receiptapp.expenses.company;

import com.receiptapp.expenses.audit.AuditEntityType;
import com.receiptapp.expenses.audit.AuditEvent;
import com.receiptapp.expenses.audit.AuditEventRepository;
import com.receiptapp.expenses.common.BusinessRuleException;
import com.receiptapp.expenses.common.CurrencyCode;
import com.receiptapp.expenses.company.dto.CompanySignupRequest;
import com.receiptapp.expenses.company.dto.CompanySignupResponse;
import com.receiptapp.expenses.security.TokenService;
import com.receiptapp.expenses.user.AppUser;
import com.receiptapp.expenses.user.AppUserRepository;
import com.receiptapp.expenses.user.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class CompanySignupService {

    private final CompanyRepository companies;
    private final AppUserRepository users;
    private final DefaultCompanySetup defaults;
    private final AuditEventRepository auditEvents;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public CompanySignupService(CompanyRepository companies, AppUserRepository users,
            DefaultCompanySetup defaults, AuditEventRepository auditEvents,
            PasswordEncoder passwordEncoder, TokenService tokens) {
        this.companies = companies;
        this.users = users;
        this.defaults = defaults;
        this.auditEvents = auditEvents;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    /**
     * Creates the company, its first admin, its default categories and fuel types,
     * and an
     * audit record, all in ONE transaction: if any part fails, none of it is saved.
     */
    @Transactional
    public CompanySignupResponse signUp(CompanySignupRequest request) {
        String email = request.adminEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("An account with this email already exists. Try logging in instead.");
        }
        String localPart = email.substring(0, email.indexOf('@'));
        if (localPart.length() >= 4 && request.password().toLowerCase(Locale.ROOT).contains(localPart)) {
            throw new BusinessRuleException("Your password shouldn't contain your email address.");
        }

        CurrencyCode currency = request.baseCurrency() != null ? request.baseCurrency() : CurrencyCode.USD;
        Company company = companies.save(new Company(request.companyName().trim(), currency));

        AppUser admin = users.save(new AppUser(
                company,
                request.adminFullName().trim(),
                email,
                blankToNull(request.adminPhone()),
                passwordEncoder.encode(request.password()),
                Role.ADMIN));

        defaults.createDefaultsFor(company);

        auditEvents.save(AuditEvent.byUser(admin, "COMPANY_REGISTERED", AuditEntityType.COMPANY,
                company.getId(), null, null, null));

        TokenService.IssuedToken token = tokens.issueFor(admin);
        return new CompanySignupResponse(company.getId(), company.getName(), admin.getId(),
                token.value(), "Bearer", token.expiresAt());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}