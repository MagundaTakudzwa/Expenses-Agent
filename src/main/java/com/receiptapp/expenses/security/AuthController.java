package com.receiptapp.expenses.security;

import com.receiptapp.expenses.security.dto.LoginRequest;
import com.receiptapp.expenses.security.dto.LoginResponse;
import com.receiptapp.expenses.security.dto.MeResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Public: exchange an email and password for a token. */
    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Any logged-in user: who am I, and which company am I in? */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        return authService.me(currentUser);
    }
}