package com.challenge.backend.auth.service;

import com.challenge.backend.auth.dto.LoginRequest;
import com.challenge.backend.auth.dto.OAuth2ExchangeRequest;
import com.challenge.backend.auth.dto.RefreshRequest;
import com.challenge.backend.auth.dto.RegisterRequest;
import com.challenge.backend.auth.dto.TokenResponse;
import com.challenge.backend.auth.exception.EmailAlreadyExistsException;
import com.challenge.backend.auth.exception.InvalidCredentialsException;
import com.challenge.backend.auth.exception.InvalidTokenException;
import com.challenge.backend.auth.exception.NicknameAlreadyExistsException;
import com.challenge.backend.auth.jwt.JwtTokenProvider;
import com.challenge.backend.auth.oauth2.OAuth2LoginCodeService;
import com.challenge.backend.user.entity.AuthProvider;
import com.challenge.backend.user.entity.ProviderType;
import com.challenge.backend.user.entity.Role;
import com.challenge.backend.user.entity.User;
import com.challenge.backend.user.repository.AuthProviderRepository;
import com.challenge.backend.user.repository.UserRepository;
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

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new NicknameAlreadyExistsException(request.nickname());
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.nickname(), Role.USER, true);
        userRepository.save(user);
        authProviderRepository.save(new AuthProvider(user, ProviderType.LOCAL, request.email()));

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
        try {
            var claims = jwtTokenProvider.parseClaims(request.refreshToken());
            if (!jwtTokenProvider.isRefreshToken(claims)) {
                throw new InvalidTokenException("refresh 토큰이 아닙니다.");
            }

            User user = userRepository.findById(jwtTokenProvider.getUserId(claims))
                    .orElseThrow(() -> new InvalidTokenException("존재하지 않는 사용자입니다."));

            return issueTokens(user);
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
