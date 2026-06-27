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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Auth", description = "회원가입 · 로그인 · 토큰 · 이메일 인증 · 소셜 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "일반 회원가입", description = "이메일·비밀번호·닉네임으로 가입. 가입 전 이메일 인증 필수.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "가입 성공 — Access/Refresh 토큰 반환"),
            @ApiResponse(responseCode = "400", description = "입력값 오류 또는 이메일 미인증"),
            @ApiResponse(responseCode = "409", description = "이메일·닉네임 중복")
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "로그인", description = "이메일·비밀번호 로그인. Access/Refresh 토큰을 발급한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Access Token 재발급", description = "만료된 Access Token을 Refresh Token으로 교환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "Refresh Token 무효 또는 만료")
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 Redis 블랙리스트에 등록해 무효화한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token")
    })
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "카카오 OAuth2 코드 교환",
            description = "카카오 로그인 후 서버에서 발급한 1회용 코드(Redis TTL 60초)를 Access/Refresh 토큰으로 교환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 발급 성공"),
            @ApiResponse(responseCode = "401", description = "코드 만료 또는 미존재")
    })
    @SecurityRequirements
    @PostMapping("/oauth2/exchange")
    public TokenResponse exchangeOAuth2Code(@Valid @RequestBody OAuth2ExchangeRequest request) {
        return authService.exchangeOAuth2Code(request);
    }

    @Operation(summary = "닉네임 중복 확인", description = "사용 가능한 닉네임인지 확인한다.")
    @ApiResponse(responseCode = "200", description = "available: true/false")
    @SecurityRequirements
    @GetMapping("/check-nickname")
    public NicknameAvailabilityResponse checkNickname(@RequestParam @NotBlank String nickname) {
        return new NicknameAvailabilityResponse(authService.isNicknameAvailable(nickname));
    }

    @Operation(summary = "이메일 중복 확인", description = "이미 가입된 이메일인지 확인한다.")
    @ApiResponse(responseCode = "200", description = "available: true/false")
    @SecurityRequirements
    @GetMapping("/check-email")
    public EmailAvailabilityResponse checkEmail(@RequestParam @NotBlank String email) {
        return new EmailAvailabilityResponse(authService.isEmailAvailable(email));
    }

    @Operation(summary = "이메일 인증 코드 발송", description = "입력한 이메일로 6자리 인증 코드를 발송한다 (Redis TTL 5분).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "발송 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 이메일")
    })
    @SecurityRequirements
    @PostMapping("/email/send-code")
    public ResponseEntity<Void> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        authService.sendEmailCode(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "이메일 인증 코드 검증", description = "발송된 코드를 검증하고 인증 완료 상태를 Redis에 기록한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "검증 성공"),
            @ApiResponse(responseCode = "400", description = "코드 불일치 또는 만료")
    })
    @SecurityRequirements
    @PostMapping("/email/verify-code")
    public ResponseEntity<Void> verifyEmailCode(@Valid @RequestBody EmailVerifyRequest request) {
        authService.verifyEmailCode(request);
        return ResponseEntity.noContent().build();
    }
}
