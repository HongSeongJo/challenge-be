package com.challenge.backend.settlement.entity;

import com.challenge.backend.challenge.entity.ChallengeParticipation;
import com.challenge.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참가자 개인별 정산 지급 내역 (audit trail).
 * 누가 얼마를 받았는지, 어떤 이유로 받았는지 기록한다.
 * ChallengeSettlement(요약)에 연결된 상세 내역 테이블.
 */
@Entity
@Table(name = "settlement_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementItem {

    /** 정산 항목 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 항목이 속한 정산 요약 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private ChallengeSettlement settlement;

    /** 지급 받는 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 이 항목과 연결된 참가 기록 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    private ChallengeParticipation participation;

    /**
     * 실제 수령/환불 금액 (원 단위).
     * REWARD: amountPerCompleter (완주 보상)
     * FULL_REFUND: depositAmount (원금 환불)
     */
    @Column(nullable = false)
    private Long receivedAmount;

    /**
     * 지급 유형.
     * REWARD: 완주자가 미완주자 보증금을 분배받음
     * FULL_REFUND: 전원 완주/실패/취소 상황에서 원금 환불
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementItemType type;

    @Builder
    private SettlementItem(ChallengeSettlement settlement, User user,
                           ChallengeParticipation participation,
                           Long receivedAmount, SettlementItemType type) {
        this.settlement = settlement;
        this.user = user;
        this.participation = participation;
        this.receivedAmount = receivedAmount;
        this.type = type;
    }
}
