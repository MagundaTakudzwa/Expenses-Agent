package com.receiptapp.expenses.security;

import com.receiptapp.expenses.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiIntegrationTest extends ApiIntegrationTest {

    @Test
    void loginReturnsATokenThatWorksForMe() {
        String email = uniqueEmail("admin");
        signUp("Login Test Co", email, PASSWORD);

        MvcTestResult login = login(email, PASSWORD);
        assertThat(login).hasStatusOk();
        assertThat(login).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
        assertThat(login).bodyJson().extractingPath("$.role").isEqualTo("ADMIN");

        String token = read(login, "$.accessToken");
        MvcTestResult me = me(token);
        assertThat(me).hasStatusOk();
        assertThat(me).bodyJson().extractingPath("$.email").isEqualTo(email);
        assertThat(me).bodyJson().extractingPath("$.companyName").isEqualTo("Login Test Co");
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() {
        String email = uniqueEmail("admin");
        signUp("Same Message Co", email, PASSWORD);

        MvcTestResult wrongPassword = login(email, "wrong-password-123");
        MvcTestResult unknownEmail = login(uniqueEmail("nobody"), PASSWORD);

        assertThat(wrongPassword).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail).hasStatus(HttpStatus.UNAUTHORIZED);
        String wrongPasswordMessage = read(wrongPassword, "$.detail");
        String unknownEmailMessage = read(unknownEmail, "$.detail");
        assertThat(unknownEmailMessage).isEqualTo(wrongPasswordMessage);
    }

    @Test
    void deactivatedUserIsOnlyToldAfterTheCorrectPassword() {
        String email = uniqueEmail("former");
        signUp("Deactivated Co", email, PASSWORD);
        jdbc.update("UPDATE app_users SET active = false WHERE email = ?", email);

        MvcTestResult rightPassword = login(email, PASSWORD);
        assertThat(rightPassword).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(rightPassword).bodyJson().extractingPath("$.detail")
                .asString().containsIgnoringCase("deactivated");

        MvcTestResult wrongPassword = login(email, "wrong-password-123");
        assertThat(wrongPassword).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword).bodyJson().extractingPath("$.detail")
                .asString().doesNotContainIgnoringCase("deactivated");
    }

    @Test
    void existingTokenStopsWorkingWhenUserIsDeactivated() {
        String email = uniqueEmail("leaver");
        String token = signUpAndGetToken(email);
        assertThat(me(token)).hasStatusOk();

        jdbc.update("UPDATE app_users SET active = false WHERE email = ?", email);

        assertThat(me(token)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void blankFieldsAreRejectedWithAListOfErrors() {
        MvcTestResult result = login("", "");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.email").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.password").isNotNull();
    }

    @Test
    void meWithoutATokenIsRejected() {
        assertThat(mvc.get().uri("/api/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = signUpAndGetToken(uniqueEmail("admin"));

        assertThat(me(token + "x")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}