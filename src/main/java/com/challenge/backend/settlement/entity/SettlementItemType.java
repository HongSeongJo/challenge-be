package com.challenge.backend.settlement.entity;

/**
 * 정산 항목(SettlementItem) 개별 지급 유형.
 * 참가자별로 어떤 이유로 돈을 받는지 audit trail(감사 기록)을 위해 구분함.
 */
public enum SettlementItemType {
    /** 완주 보상 — 미완주자의 보증금을 분배받은 완주자에게 지급 */
    REWARD,
    /** 원금 환불 — 전원 완주/실패/취소 상황에서 각자 낸 보증금 그대로 돌려줌 */
    FULL_REFUND
}
