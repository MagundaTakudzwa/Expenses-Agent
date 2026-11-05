package com.receiptapp.expenses.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The app.jwt.* settings from application.properties, loaded into one object.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {
}