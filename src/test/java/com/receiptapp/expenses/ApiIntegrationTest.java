package com.receiptapp.expenses;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Base class for tests that call the real API: the whole app, real security,
 * and a real
 * PostgreSQL database (expenses_test). Every test creates its own company with
 * a unique
 * email, so tests never depend on each other or on data left over from an
 * earlier run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiIntegrationTest {

    protected static final String PASSWORD = "Integration-Test-2026";

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    /** e.g. chiedza@t-3f2a9c...co.zw: readable before the @, unique after it. */
    protected static String uniqueEmail(String name) {
        return name + "@t-" + UUID.randomUUID() + ".co.zw";
    }

    protected MvcTestResult postJson(String uri, String json) {
        return mvc.post().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    protected MvcTestResult signUp(String companyName, String email, String password) {
        return postJson("/api/companies", """
                {
                  "companyName": "%s",
                  "adminFullName": "Test Admin",
                  "adminEmail": "%s",
                  "password": "%s"
                }
                """.formatted(companyName, email, password));
    }

    protected MvcTestResult login(String email, String password) {
        return postJson("/api/auth/login", """
                { "email": "%s", "password": "%s" }
                """.formatted(email, password));
    }

    protected MvcTestResult me(String token) {
        return mvc.get().uri("/api/me")
                .header("Authorization", "Bearer " + token)
                .exchange();
    }

    /** Signs up a fresh company and returns its admin's access token. */
    protected String signUpAndGetToken(String email) {
        return read(signUp("Test Co", email, PASSWORD), "$.accessToken");
    }

    /**
     * Reads one value out of a JSON response, e.g. read(result, "$.accessToken").
     */
    protected static <T> T read(MvcTestResult result, String jsonPath) {
        try {
            return JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    protected long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }
}