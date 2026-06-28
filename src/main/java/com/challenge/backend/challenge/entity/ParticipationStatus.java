package com.challenge.backend.challenge.entity;

/**
 * 챌린지 참가자 개인의 완주 여부 상태.
 * 챌린지 종료 시 completionThresholdPct 기준으로 COMPLETED / FAILED 로 분류됨.
 */
public enum ParticipationStatus {
    /** 진행 중 — 아직 챌린지가 끝나지 않아 완주 여부 미결정 */
    ACTIVE,
    /** 완주 — 설정된 달성률(completionThresholdPct) 이상 인증 성공 */
    COMPLETED,
    /** 미완주 — 달성률 미달. 보증금이 완주자에게 분배됨 */
    FAILED
}
