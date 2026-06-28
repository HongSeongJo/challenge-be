package com.challenge.backend.verification.service;

import com.challenge.backend.challenge.entity.Challenge;
import com.challenge.backend.user.entity.User;
import org.springframework.stereotype.Service;

/**
 * VerificationQueryService 임시 구현체.
 * 3단계에서 실제 인증샷 도메인 구현 시 이 클래스를 삭제하고 실제 구현체로 교체한다.
 * TODO: 3단계 완료 후 제거
 */
@Service
public class StubVerificationQueryService implements VerificationQueryService {

    @Override
    public int countVerified(Challenge challenge, User user) {
        // 3단계 구현 전까지 0 반환 → 정산 시 전원 미완주 처리됨
        return 0;
    }
}
