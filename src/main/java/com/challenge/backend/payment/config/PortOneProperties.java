package com.challenge.backend.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.portone")
public record PortOneProperties(
        String apiKey,
        String apiSecret,
        String restApiUrl
) {}
