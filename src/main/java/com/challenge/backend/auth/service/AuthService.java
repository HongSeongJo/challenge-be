package com.challenge.backend.auth.service;

import com.challenge.backend.auth.dto.EmailCodeRequest;
import com.challenge.backend.auth.dto.EmailVerifyRequest;
import com.challenge.backend.auth.dto.LoginRequest;
import com.challenge.backend.auth.dto.OAuth2ExchangeRequest;
import com.challenge.backend.auth.dto.RefreshRequest;
import com.challenge.backend.auth.dto.RegisterRequest;
import com.challenge.backend.auth.dto.TokenResponse;
import com.challenge.backend.auth.email.EmailSender;
import com.challenge.backend.auth.email.EmailVerificationService;
import com.challenge.backend.auth.exception.EmailAlreadyExistsException;
import com.challenge.backend.auth.exception.EmailNotVerifiedException;
import com.challenge.backend.auth.exception.InvalidCredentialsException;
import com.challenge.backend.auth.exception.InvalidEmailCodeException;
import com.challenge.backend.auth.exception.InvalidTokenException;
import com.challenge.backend.auth.exception.NicknameAlreadyExistsException;
import com.challenge.backend.auth.jwt.JwtTokenProvider;
import com.challenge.backend.auth.jwt.RefreshTokenBlacklistService;
import com.challenge.backend.auth.oauth2.OAuth2LoginCodeService;
import com.challenge.backend.user.entity.AuthProvider;
import com.challenge.backend.user.entity.ProviderType;
import com.challenge.backend.user.entity.Role;
import com.challenge.backend.user.entity.User;
import com.challenge.backend.user.repository.AuthProviderRepository;
import com.challenge.backend.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2LoginCodeService oAuth2LoginCodeService;
    private final RefreshTokenBlacklistService refreshTokenBlacklistService;
    private final EmailVerificationService emailVerificationService;
    private final EmailSender emailSender;

    /** 이미 가입된 이메일이면 인증 코드를 보낼 필요가 없으므로 먼저 막는다. */
    public void sendEmailCode(EmailCodeRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        String code = emailVerificationService.issueCode(request.email());
        emailSender.sendVerificationCode(request.email(), code);
    }

    public void verifyEmailCode(EmailVerifyRequest request) {
        if (!emailVerificationService.verifyCode(request.email(), request.code())) {
            throw new InvalidEmailCodeException();
        }
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new NicknameAlreadyExistsException(request.nickname());
        }
        if (!emailVerificationService.isVerified(request.email())) {
            throw new EmailNotVerifiedException();
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.nickname(), Role.USER, true);
        userRepository.save(user);
        authProviderRepository.save(new AuthProvider(user, ProviderType.LOCAL, request.email()));
        emailVerificationService.consumeVerified(request.email());

        return issueTokens(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.hasPassword() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    public TokenResponse refresh(RefreshRequest request) {
        Claims claims = parseRefreshTokenOrThrow(request.refreshToken());

        User user = userRepository.findById(jwtTokenProvider.getUserId(claims))
                .orElseThrow(() -> new InvalidTokenException("존재하지 않는 사용자입니다."));

        return issueTokens(user);
    }

    /** refresh token을 jti 기준으로 블랙리스트에 올려 더 이상 갱신에 쓸 수 없게 만든다. */
    public void logout(RefreshRequest request) {
        Claims claims = parseRefreshTokenOrThrow(request.refreshToken());
        refreshTokenBlacklistService.blacklist(jwtTokenProvider.getTokenId(claims), jwtTokenProvider.getExpiration(claims));
    }

    private Claims parseRefreshTokenOrThrow(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            if (!jwtTokenProvider.isRefreshToken(claims)) {
                throw new InvalidTokenException("refresh 토큰이 아닙니다.");
            }
            if (refreshTokenBlacklistService.isBlacklisted(jwtTokenProvider.getTokenId(claims))) {
                throw new InvalidTokenException("로그아웃된 토큰입니다.");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("유효하지 않은 토큰입니다.");
        }
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    /**
     * 카카오 로그인 성공 후 발급된 1회용 코드를 실제 JWT로 교환한다.
     * 코드는 Redis에서 즉시 삭제되어 재사용할 수 없다.
     */
    public TokenResponse exchangeOAuth2Code(OAuth2ExchangeRequest request) {
        Long userId = oAuth2LoginCodeService.consumeCode(request.code());
        if (userId == null) {
            throw new InvalidTokenException("유효하지 않거나 만료된 코드입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("존재하지 않는 사용자입니다."));

        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole());
        return new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirySeconds(), user.isNicknameConfirmed());
    }
}
