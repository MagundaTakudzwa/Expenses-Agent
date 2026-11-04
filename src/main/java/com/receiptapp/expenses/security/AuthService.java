package com.receiptapp.expenses.security;

import com.receiptapp.expenses.common.NotFoundException;
import com.receiptapp.expenses.company.Company;
import com.receiptapp.expenses.company.CompanyRepository;
import com.receiptapp.expenses.security.dto.LoginRequest;
import com.receiptapp.expenses.security.dto.LoginResponse;
import com.receiptapp.expenses.security.dto.MeResponse;
import com.receiptapp.expenses.user.AppUser;
import com.receiptapp.expenses.user.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    // The same message whether the email or the password is wrong.
    private static final String WRONG_LOGIN = "Incorrect email or password.";

    private final AppUserRepository users;
    private final CompanyRepository companies;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final String dummyHash;

    public AuthService(AppUserRepository users, CompanyRepository companies,
            PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.companies = companies;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        // Used to spend the same time checking a password when the email doesn't exist.
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<AppUser> found = users.findByEmailIgnoreCase(request.email().trim());
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new BadCredentialsException(WRONG_LOGIN);
        }
        AppUser user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(WRONG_LOGIN);
        }
        // Only revealed AFTER the right password, so it tells an attacker nothing.
        if (!user.isActive()) {
            throw new DisabledException("This account has been deactivated. Please contact your administrator.");
        }
        TokenService.IssuedToken token = tokens.issueFor(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresAt(),
                user.getId(), user.getFullName(), user.getRole());
    }

    @Transactional(readOnly = true)
    public MeResponse me(CurrentUser currentUser) {
        AppUser user = users.findByIdAndCompanyId(currentUser.id(), currentUser.companyId())
                .orElseThrow(() -> new NotFoundException("User not found."));
        Company company = companies.findById(currentUser.companyId())
                .orElseThrow(() -> new NotFoundException("Company not found."));
        return new MeResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                company.getId(), company.getName());
    }
}