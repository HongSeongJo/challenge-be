package com.challenge.backend.challenge.entity;

/**
 * 챌린지의 진행 상태를 나타내는 열거형.
 * 상태 전환 흐름: RECRUITING → IN_PROGRESS → COMPLETED
 *                                    ↘ CANCELLED (전원 동의 시)
 */
public enum ChallengeStatus {
    /** 모집 중 — 참가자를 받고 있는 상태. 아직 시작일이 되지 않음 */
    RECRUITING,
    /** 진행 중 — 시작일이 지나 챌린지가 본격적으로 진행되는 상태 */
    IN_PROGRESS,
    /** 완료 — 종료일이 지나 챌린지가 끝난 상태. 정산이 진행됨 */
    COMPLETED,
    /** 취소 — 전원 동의로 챌린지가 중단된 상태. 전액 환불 처리됨 */
    CANCELLED
}
