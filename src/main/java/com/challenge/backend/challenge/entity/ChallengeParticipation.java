package com.challenge.backend.challenge.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 챌린지 참가 정보를 나타내는 엔티티.
 * 한 사용자가 한 챌린지에 한 번만 참가할 수 있다 (challenge_id + user_id 복합 유니크).
 * 결제 정보는 별도 Payment 테이블에서 관리 (정규화).
 */
@Entity
@Table(
    name = "challenge_participations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"challenge_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeParticipation {

    /** 참가 기록 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 참가한 챌린지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    /** 참가한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 완주 여부 상태.
     * ACTIVE: 챌린지 진행 중 (아직 결과 미결정)
     * COMPLETED: 완주 (completionThresholdPct 이상 달성)
     * FAILED: 미완주 (달성률 미달, 보증금 몰수)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status;

    /**
     * 전원 동의 즉시 취소 투표 여부.
     * 모든 ACTIVE 참가자가 true가 되면 챌린지가 즉시 CANCELLED 되고 전액 환불된다.
     */
    @Column(nullable = false)
    private boolean cancelVoted;

    /** 챌린지 참가 일시 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    private void prePersist() {
        this.joinedAt = LocalDateTime.now();
        if (this.status == null) this.status = ParticipationStatus.ACTIVE;
        this.cancelVoted = false;
    }

    @Builder
    private ChallengeParticipation(Challenge challenge, User user) {
        this.challenge = challenge;
        this.user = user;
    }

    /** 취소 투표를 한다. 전원 투표 완료 시 서비스 레이어에서 챌린지 CANCELLED 처리 */
    public void voteCancellation() {
        this.cancelVoted = true;
    }

    /** 완주 처리 — 종료 후 달성률 계산 결과로 호출됨 */
    public void complete() {
        this.status = ParticipationStatus.COMPLETED;
    }

    /** 미완주 처리 — 달성률 미달 시 호출됨 */
    public void fail() {
        this.status = ParticipationStatus.FAILED;
    }
}
