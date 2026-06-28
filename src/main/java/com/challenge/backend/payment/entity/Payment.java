package com.challenge.backend.payment.entity;

import com.challenge.backend.challenge.entity.ChallengeParticipation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보증금 결제 내역을 나타내는 엔티티.
 * ChallengeParticipation과 1:1 관계 — 참가 1건당 결제 1건.
 * 참가 정보(완주 여부 등)와 결제 정보를 분리해 정규화함.
 * 추후 재결제, 부분 환불, 다양한 결제 수단 추가 시 이 테이블만 확장하면 된다.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    /** 결제 기록 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 결제가 연결된 참가 기록 (1:1) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false, unique = true)
    private ChallengeParticipation participation;

    /**
     * 결제 방식.
     * MANUAL: 방장이 카카오페이/토스 입금 확인 후 수동으로 승인 (현재)
     * PORTONE: 포트원 PG 자동 결제 (사업자 등록 후 전환 예정)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    /** 결제 금액 (원 단위) — 챌린지의 depositAmount와 같아야 함 */
    @Column(nullable = false)
    private Long amount;

    /**
     * 현재 결제 상태.
     * PENDING → PAID → REFUNDED 순으로 전이됨.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * 우리 서버에서 생성한 주문 번호 — PORTONE 방식에서 포트원 결제창 호출 시 전달.
     * TODO: PORTONE 전환 시 not null로 변경
     */
    private String merchantUid;

    /**
     * 포트원(아임포트) 결제 고유 ID — 결제 완료 후 포트원이 반환.
     * MANUAL 방식에서는 사용하지 않음.
     * TODO: PORTONE 전환 시 활성화
     */
    private String impUid;

    /** 결제 기록 생성 일시 (참가 신청 시점) */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 실제 결제 완료 일시.
     * MANUAL: 방장이 입금 확인한 시각
     * PORTONE: 포트원 검증이 완료된 시각
     */
    private LocalDateTime paidAt;

    /** 환불 처리 완료 일시 */
    private LocalDateTime refundedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = PaymentStatus.PENDING;
    }

    /** MANUAL 결제 기록 생성 — 참가자가 송금 후 방장 확인 대기 상태로 시작 */
    @Builder(builderMethodName = "manualBuilder", buildMethodName = "build")
    private Payment(ChallengeParticipation participation, Long amount) {
        this.participation = participation;
        this.type = PaymentType.MANUAL;
        this.amount = amount;
    }

    /**
     * PORTONE 결제 기록 생성 — merchantUid 발급 후 PENDING 상태로 시작.
     * TODO: PORTONE 전환 시 활성화
     */
    @Builder(builderMethodName = "portoneBuilder", buildMethodName = "build")
    private Payment(ChallengeParticipation participation, Long amount, String merchantUid) {
        this.participation = participation;
        this.type = PaymentType.PORTONE;
        this.amount = amount;
        this.merchantUid = merchantUid;
    }

    /** MANUAL 방식 — 방장이 입금을 확인하고 승인할 때 호출 */
    public void confirmManual() {
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    /** PORTONE 방식 — 포트원 검증 성공 후 결제 확정 시 호출 */
    public void confirmPortone(String impUid) {
        this.impUid = impUid;
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    /** 환불 처리 — 참가 취소 또는 챌린지 취소 시 호출 */
    public void refund() {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    /** 취소 — 환불 없이 결제 무효화 (예: 결제창 이탈) */
    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }
}
