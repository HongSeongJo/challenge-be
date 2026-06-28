package com.challenge.backend.payment.entity;

/**
 * 보증금 결제 방식.
 * 현재는 MANUAL만 사용하고, 사업자 등록 후 PORTONE으로 전환 예정.
 */
public enum PaymentType {
    /** 수동 결제 — 참가자가 방장에게 카카오페이/토스 등으로 직접 송금 후 방장이 앱에서 확인 */
    MANUAL,
    /**
     * 포트원(아임포트) PG 자동 결제 — 앱 내 결제창으로 자동 처리.
     * TODO: 사업자 등록 완료 후 활성화
     */
    PORTONE
}
