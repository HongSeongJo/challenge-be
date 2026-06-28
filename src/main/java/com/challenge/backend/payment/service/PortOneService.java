package com.challenge.backend.payment.service;

import com.challenge.backend.payment.config.PortOneProperties;
import com.challenge.backend.payment.exception.PaymentVerificationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PortOneService {

    private final PortOneProperties portOneProperties;
    private final RestClient restClient = RestClient.create();

    /** 포트원 액세스 토큰 발급 */
    private String getAccessToken() {
        var response = restClient.post()
                .uri(portOneProperties.restApiUrl() + "/users/getToken")
                .body(new TokenRequest(portOneProperties.apiKey(), portOneProperties.apiSecret()))
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.response() == null) {
            throw new PaymentVerificationException("포트원 토큰 발급 실패");
        }
        return response.response().accessToken();
    }

    /**
     * 결제 검증 — impUid로 포트원 결제 정보 조회 후 금액 대조
     * @param impUid   포트원 결제 고유 ID
     * @param expected 우리 서버에서 기대하는 금액
     */
    public void verify(String impUid, Long expected) {
        String token = getAccessToken();

        var response = restClient.get()
                .uri(portOneProperties.restApiUrl() + "/payments/" + impUid)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(PaymentResponse.class);

        if (response == null || response.response() == null) {
            throw new PaymentVerificationException("결제 정보를 조회할 수 없습니다.");
        }

        var payment = response.response();

        if (!"paid".equals(payment.status())) {
            throw new PaymentVerificationException("결제 상태가 유효하지 않습니다: " + payment.status());
        }

        if (!expected.equals(payment.amount())) {
            throw new PaymentVerificationException(
                    "결제 금액 불일치 (기대: %d, 실제: %d)".formatted(expected, payment.amount()));
        }
    }

    /**
     * 포트원 환불 요청
     * @param impUid  환불할 결제의 포트원 ID
     * @param amount  환불 금액 (null이면 전액 환불)
     * @param reason  환불 사유
     */
    public void refund(String impUid, Long amount, String reason) {
        String token = getAccessToken();

        restClient.post()
                .uri(portOneProperties.restApiUrl() + "/payments/cancel")
                .header("Authorization", "Bearer " + token)
                .body(new RefundRequest(impUid, amount, reason))
                .retrieve()
                .toBodilessEntity();
    }

    // ── 내부 DTO ──────────────────────────────────────────

    private record TokenRequest(
            @JsonProperty("imp_key") String impKey,
            @JsonProperty("imp_secret") String impSecret) {}

    private record TokenResponse(TokenData response) {
        private record TokenData(@JsonProperty("access_token") String accessToken) {}
    }

    private record PaymentResponse(PaymentData response) {
        private record PaymentData(String status, Long amount) {}
    }

    private record RefundRequest(
            @JsonProperty("imp_uid") String impUid,
            @JsonProperty("cancel_request_amount") Long cancelRequestAmount,
            String reason) {}
}
