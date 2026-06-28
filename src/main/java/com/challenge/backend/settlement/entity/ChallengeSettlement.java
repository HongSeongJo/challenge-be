package com.challenge.backend.settlement.entity;

import com.challenge.backend.challenge.entity.Challenge;
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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 챌린지 종료 후 정산 요약 정보를 기록하는 엔티티.
 * 챌린지 1개당 정산 요약 1개 (1:1).
 * 개별 참가자의 지급 내역은 SettlementItem에서 별도 관리 (audit trail).
 */
@Entity
@Table(name = "challenge_settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeSettlement {

    /** 정산 요약 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 정산이 완료된 챌린지 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false, unique = true)
    private Challenge challenge;

    /**
     * 정산 방식.
     * NORMAL: 일부 완주, 일부 실패 → 미완주자 보증금을 완주자에게 분배
     * FULL_REFUND: 전원 완주 / 전원 실패 / 전원 동의 취소 → 각자 원금 환불
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementType type;

    /**
     * 전체 보증금 풀 = 결제 완료(PAID) 참가자 수 × depositAmount.
     * 예: 10명 참가 × 10,000원 = 100,000원
     */
    @Column(nullable = false)
    private Long totalPool;

    /** 완주자 수 */
    @Column(nullable = false)
    private int completedCount;

    /** 미완주자 수 */
    @Column(nullable = false)
    private int failedCount;

    /**
     * 완주자(또는 전액 환불 대상자) 1인당 수령 금액.
     * NORMAL: totalPool × (1 - platformFeeRate) / completedCount
     * FULL_REFUND: depositAmount (원금 그대로)
     */
    @Column(nullable = false)
    private Long amountPerCompleter;

    /**
     * 플랫폼 수수료율 (0.0 ~ 1.0). 현재 0으로 고정.
     * 예: 0.05 → 5% 수수료
     * TODO: 사업자 등록 후 수수료 정책 도입 시 변경
     */
    @Column(nullable = false)
    private double platformFeeRate;

    /** 정산이 처리된 일시 */
    @Column(nullable = false)
    private LocalDateTime settledAt;

    @Builder
    private ChallengeSettlement(Challenge challenge, SettlementType type,
                                Long totalPool, int completedCount, int failedCount,
                                Long amountPerCompleter) {
        this.challenge = challenge;
        this.type = type;
        this.totalPool = totalPool;
        this.completedCount = completedCount;
        this.failedCount = failedCount;
        this.amountPerCompleter = amountPerCompleter;
        this.platformFeeRate = 0.0;
        this.settledAt = LocalDateTime.now();
    }
}
