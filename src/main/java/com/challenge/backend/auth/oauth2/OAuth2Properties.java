package com.challenge.backend.auth.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2Properties(String successRedirectUri) {
}
