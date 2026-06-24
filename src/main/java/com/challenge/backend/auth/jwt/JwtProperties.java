package com.challenge.backend.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirySeconds,
        long refreshTokenExpirySeconds
) {
}
