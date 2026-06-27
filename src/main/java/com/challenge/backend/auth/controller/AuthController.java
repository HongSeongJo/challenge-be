package com.challenge.backend.auth.controller;

import com.challenge.backend.auth.dto.EmailAvailabilityResponse;
import com.challenge.backend.auth.dto.EmailCodeRequest;
import com.challenge.backend.auth.dto.EmailVerifyRequest;
import com.challenge.backend.auth.dto.LoginRequest;
import com.challenge.backend.auth.dto.NicknameAvailabilityResponse;
import com.challenge.backend.auth.dto.OAuth2ExchangeRequest;
import com.challenge.backend.auth.dto.RefreshRequest;
import com.challenge.backend.auth.dto.RegisterRequest;
import com.challenge.backend.auth.dto.TokenResponse;
import com.challenge.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth2/exchange")
    public TokenResponse exchangeOAuth2Code(@Valid @RequestBody OAuth2ExchangeRequest request) {
        return authService.exchangeOAuth2Code(request);
    }

    @GetMapping("/check-nickname")
    public NicknameAvailabilityResponse checkNickname(@RequestParam @NotBlank String nickname) {
        return new NicknameAvailabilityResponse(authService.isNicknameAvailable(nickname));
    }

    @GetMapping("/check-email")
    public EmailAvailabilityResponse checkEmail(@RequestParam @NotBlank String email) {
        return new EmailAvailabilityResponse(authService.isEmailAvailable(email));
    }

    @PostMapping("/email/send-code")
    public ResponseEntity<Void> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        authService.sendEmailCode(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/verify-code")
    public ResponseEntity<Void> verifyEmailCode(@Valid @RequestBody EmailVerifyRequest request) {
        authService.verifyEmailCode(request);
        return ResponseEntity.noContent().build();
    }
}
