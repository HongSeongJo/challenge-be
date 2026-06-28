package com.challenge.backend.verification.service;

import com.challenge.backend.challenge.entity.Challenge;
import com.challenge.backend.user.entity.User;

/**
 * 인증샷 조회 서비스 인터페이스.
 * 실제 구현은 3단계(인증샷 도메인)에서 완성된다.
 * 현재는 정산 서비스에서 완주 판정에 필요한 인증 횟수를 조회하기 위한 구조만 잡아둠.
 */
public interface VerificationQueryService {

    /**
     * 특정 챌린지에서 특정 사용자의 인증 완료 횟수를 반환한다.
     * TODO: 3단계에서 Verification 도메인 구현 시 이 인터페이스를 구현체로 교체
     */
    int countVerified(Challenge challenge, User user);
}
