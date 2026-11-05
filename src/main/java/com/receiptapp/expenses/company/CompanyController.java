package com.receiptapp.expenses.company;

import com.receiptapp.expenses.company.dto.CompanySignupRequest;
import com.receiptapp.expenses.company.dto.CompanySignupResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanySignupService signupService;

    public CompanyController(CompanySignupService signupService) {
        this.signupService = signupService;
    }

    /**
     * Public: a new business signs up (allowed without a token in SecurityConfig).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanySignupResponse signUp(@Valid @RequestBody CompanySignupRequest request) {
        return signupService.signUp(request);
    }
}