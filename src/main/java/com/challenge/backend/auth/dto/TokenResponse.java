package com.challenge.backend.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds, boolean nicknameConfirmed) {
}
