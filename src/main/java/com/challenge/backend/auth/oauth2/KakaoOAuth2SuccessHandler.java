package com.challenge.backend.auth.oauth2;

import com.challenge.backend.user.entity.User;
import com.challenge.backend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 카카오 로그인 성공 시 (account_email 동의 필수) 이메일 기준으로 계정을 찾거나 만들고,
 * 1회용 임시 코드를 발급해 프론트엔드 콜백 URL로 리다이렉트한다.
 * 실제 JWT는 URL에 노출하지 않고, FE가 이 코드를 /api/auth/oauth2/exchange 로 교환해서 받는다.
 */
@Component
@RequiredArgsConstructor
public class KakaoOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final OAuth2LoginCodeService oAuth2LoginCodeService;
    private final OAuth2Properties oAuth2Properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerUserId = String.valueOf(attributes.get("id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null || kakaoAccount.get("email") == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "카카오 계정의 이메일 제공에 동의해야 합니다.");
            return;
        }
        String email = (String) kakaoAccount.get("email");

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String nickname = (profile != null && profile.get("nickname") != null)
                ? (String) profile.get("nickname")
                : "kakao_" + providerUserId;

        User user = userService.findOrCreateForKakao(providerUserId, email, nickname);
        String code = oAuth2LoginCodeService.issueCode(user.getId());

        String redirectUrl = UriComponentsBuilder.fromUriString(oAuth2Properties.successRedirectUri())
                .queryParam("code", code)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
