package com.challenge.backend.auth.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 회원탈퇴 시 카카오 쪽 연결도 끊는다(revoke). 사용자의 카카오 액세스 토큰을 우리가 저장해두지 않으므로,
 * 사용자 토큰 대신 앱의 Admin 키로 서버 간(server-to-server) unlink를 호출한다.
 * (카카오 디벨로퍼스 콘솔 > 앱 설정 > 보안 > Admin 키)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoUnlinkClient {

    private static final String UNLINK_URI = "https://kapi.kakao.com/v1/user/unlink";

    private final KakaoProperties kakaoProperties;
    private final RestClient restClient = RestClient.create();

    public void unlink(String providerUserId) {
        if (kakaoProperties.adminKey() == null || kakaoProperties.adminKey().isBlank()) {
            log.warn("KAKAO_ADMIN_KEY가 설정되지 않아 카카오 unlink를 건너뜁니다. providerUserId={}", providerUserId);
            return;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", providerUserId);

        try {
            restClient.post()
                    .uri(UNLINK_URI)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoProperties.adminKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            log.info("카카오 unlink 성공. providerUserId={}", providerUserId);
        } catch (Exception e) {
            // 탈퇴 자체는 우리 서비스 데이터 삭제가 핵심이므로, 카카오 쪽 연동 해제가 실패해도 탈퇴를 막지 않는다.
            log.warn("카카오 unlink 실패. providerUserId={}", providerUserId, e);
        }
    }
}
