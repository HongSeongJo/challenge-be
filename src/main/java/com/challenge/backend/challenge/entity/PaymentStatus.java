package com.challenge.backend.challenge.entity;

public enum PaymentStatus {
    PENDING,   // 결제 대기
    PAID,      // 결제 완료
    REFUNDED,  // 환불 완료
    CANCELLED  // 취소 (환불 없이)
}
