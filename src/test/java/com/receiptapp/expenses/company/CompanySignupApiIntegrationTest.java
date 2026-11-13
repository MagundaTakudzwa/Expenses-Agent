package com.receiptapp.expenses.company;

import com.receiptapp.expenses.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CompanySignupApiIntegrationTest extends ApiIntegrationTest {

    @Test
    void signUpCreatesCompanyAdminDefaultsAndAuditEvent() {
        String email = uniqueEmail("chiedza");

        MvcTestResult result = signUp("Chiedza Logistics", email, PASSWORD);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.companyName").isEqualTo("Chiedza Logistics");
        assertThat(result).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();

        // What you used to check by hand in psql:
        Number companyId = read(result, "$.companyId");
        assertThat(jdbc.queryForObject("SELECT base_currency FROM companies WHERE id = ?",
                String.class, companyId.longValue())).isEqualTo("USD");
        assertThat(count("SELECT count(*) FROM expense_categories WHERE company_id = ?",
                companyId.longValue())).isEqualTo(13);
        assertThat(count("SELECT count(*) FROM fuel_types WHERE company_id = ?",
                companyId.longValue())).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM audit_events WHERE company_id = ? AND action = 'COMPANY_REGISTERED'",
                companyId.longValue())).isEqualTo(1);

        String hash = jdbc.queryForObject("SELECT password_hash FROM app_users WHERE email = ?",
                String.class, email);
        assertThat(hash).startsWith("{bcrypt}").doesNotContain(PASSWORD);
    }

    @Test
    void signUpTokenWorksStraightAway() {
        String token = signUpAndGetToken(uniqueEmail("chiedza"));

        MvcTestResult me = me(token);

        assertThat(me).hasStatusOk();
        assertThat(me).bodyJson().extractingPath("$.role").isEqualTo("ADMIN");
    }

    @Test
    void emailIsStoredInLowercaseAndLoginIgnoresCase() {
        String email = uniqueEmail("chiedza");
        signUp("Case Test Co", email.toUpperCase(Locale.ROOT), PASSWORD);

        assertThat(count("SELECT count(*) FROM app_users WHERE email = ?", email)).isEqualTo(1);
        assertThat(login(email.toUpperCase(Locale.ROOT), PASSWORD)).hasStatusOk();
        assertThat(login(email, PASSWORD)).hasStatusOk();
    }

    @Test
    void sameEmailCannotSignUpTwice() {
        String email = uniqueEmail("chiedza");
        signUp("First Co", email, PASSWORD);

        MvcTestResult second = signUp("Copycat Ltd", email, "Another-Pass-2026");

        assertThat(second).hasStatus(422);
        assertThat(second).bodyJson().extractingPath("$.detail")
                .asString().containsIgnoringCase("already exists");
        assertThat(count("SELECT count(*) FROM app_users WHERE email = ?", email)).isEqualTo(1);
    }

    @Test
    void invalidFieldsAreListed() {
        MvcTestResult result = postJson("/api/companies", """
                { "companyName": "", "adminFullName": "Tendai", "adminEmail": "%s", "password": "short" }
                """.formatted(uniqueEmail("tendai")));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.companyName").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.password").isNotNull();
    }

    @Test
    void passwordMayNotContainTheEmail() {
        MvcTestResult result = signUp("Tendai Builders", uniqueEmail("tendai"), "tendai-2026-site");

        assertThat(result).hasStatus(422);
    }

    @Test
    void unknownCurrencyIsRejected() {
        MvcTestResult result = postJson("/api/companies", """
                { "companyName": "Rand Traders", "baseCurrency": "GBP", "adminFullName": "Sipho Dube",
                  "adminEmail": "%s", "password": "Beitbridge-Run-2026" }
                """.formatted(uniqueEmail("sipho")));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }
}