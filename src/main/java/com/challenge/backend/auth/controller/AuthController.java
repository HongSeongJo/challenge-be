package com.challenge.backend.auth.controller;

import com.challenge.backend.auth.dto.LoginRequest;
import com.challenge.backend.auth.dto.OAuth2ExchangeRequest;
import com.challenge.backend.auth.dto.RefreshRequest;
import com.challenge.backend.auth.dto.RegisterRequest;
import com.challenge.backend.auth.dto.TokenResponse;
import com.challenge.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/oauth2/exchange")
    public TokenResponse exchangeOAuth2Code(@Valid @RequestBody OAuth2ExchangeRequest request) {
        return authService.exchangeOAuth2Code(request);
    }
}
