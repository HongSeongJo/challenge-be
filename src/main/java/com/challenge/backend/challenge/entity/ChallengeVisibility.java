package com.challenge.backend.challenge.entity;

/**
 * 챌린지 공개 범위.
 * 현재는 PRIVATE만 구현. PUBLIC은 2.5단계에서 공개 챌린지 탐색 기능과 함께 오픈 예정.
 */
public enum ChallengeVisibility {
    /** 비공개 — 초대 코드(12자리)를 아는 사람만 참가 가능 */
    PRIVATE,
    /**
     * 공개 — 누구나 챌린지 목록에서 검색·참가 가능. 당근 모임처럼 같은 목표를 가진 낯선 사람들도 참가.
     * TODO: 2.5단계에서 공개 챌린지 목록/검색 API 오픈 시 활성화
     */
    PUBLIC
}
