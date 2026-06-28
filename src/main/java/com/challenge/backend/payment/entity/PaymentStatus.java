package com.challenge.backend.payment.entity;

/**
 * 결제 건의 현재 상태.
 * MANUAL 방식: PENDING → (방장 확인) → PAID → (환불 사유 발생 시) → REFUNDED
 * PORTONE 방식: PENDING → (결제창 완료·검증) → PAID → REFUNDED
 */
public enum PaymentStatus {
    /** 결제 대기 — 참가 신청은 됐지만 아직 입금 확인 전 */
    PENDING,
    /** 결제 완료 — 입금이 확인된 상태 (MANUAL: 방장 확인 / PORTONE: 포트원 검증) */
    PAID,
    /** 환불 완료 — 참가 취소 또는 챌린지 취소로 보증금이 돌아간 상태 */
    REFUNDED,
    /** 취소 — 환불 없이 결제가 무효화된 상태 (예: 결제창 이탈 후 미결제) */
    CANCELLED
}
