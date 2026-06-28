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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 챌린지 방(방) 자체를 나타내는 엔티티.
 * 하나의 챌린지에 여러 참가자(ChallengeParticipation)가 연결된다.
 * 예: "30일 아침 운동 챌린지 — 보증금 10,000원 — 완주 기준 80%"
 */
@Entity
@Table(name = "challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Challenge {

    /** 챌린지 고유 식별자 (DB 자동 생성) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 챌린지 제목. 예: "30일 새벽 6시 기상 챌린지" */
    @Column(nullable = false, length = 100)
    private String title;

    /** 챌린지 상세 설명. 규칙, 인증 방법 등 자유 기재 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 방장 — 챌린지를 생성한 사용자. 방장 위임 시 변경 가능 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User host;

    /**
     * 챌린지 기간 (일 단위). 생성 시 설정.
     * 방장이 시작 버튼을 누르면 startDate / endDate가 이 값을 기준으로 계산된다.
     */
    @Column(nullable = false)
    private int durationDays;

    /**
     * 챌린지 시작일. 방장이 시작 버튼(POST /api/challenges/{id}/start)을 누른 날.
     * 시작 전이면 null.
     */
    @Column
    private LocalDate startDate;

    /**
     * 챌린지 종료일 = startDate + durationDays - 1.
     * 이 날 이후 스케줄러가 COMPLETED로 전환 후 정산 진행.
     * 시작 전이면 null.
     */
    @Column
    private LocalDate endDate;

    /** 참가 보증금 (원 단위). 예: 10000 → 1만원 */
    @Column(nullable = false)
    private Long depositAmount;

    /** 최대 참가 인원. null이면 인원 제한 없음 */
    private Integer maxParticipants;

    /**
     * 완주 기준 달성률 (1~100, 단위: %).
     * 예: 80 → 전체 인증 횟수 중 80% 이상 인증해야 완주로 인정.
     * 30일 챌린지라면 24일(80%) 이상 인증 필요.
     */
    @Column(nullable = false)
    private int completionThresholdPct;

    /** 현재 챌린지 상태 (RECRUITING → IN_PROGRESS → COMPLETED / CANCELLED) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeStatus status;

    /**
     * 공개 범위.
     * PRIVATE: 초대 코드로만 참가 (현재 구현)
     * PUBLIC: 누구나 검색·참가 가능 (2.5단계 이후 오픈)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeVisibility visibility;

    /**
     * 초대 코드 — 12자리 랜덤 영숫자 (UUID 기반).
     * 이 코드를 아는 사람만 챌린지에 참가할 수 있다.
     * 예: "A3F9K2XM1B7C"
     */
    @Column(unique = true, length = 12, nullable = false)
    private String inviteCode;

    /**
     * 인증 빈도 — 현재 "DAILY" 고정 (매일 1회 인증).
     * TODO: 향후 WEEKLY 등 확장 시 별도 enum 타입으로 전환
     */
    @Column(nullable = false, length = 20)
    private String verificationFrequency;

    /** 챌린지 생성 일시 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = ChallengeStatus.RECRUITING;
        if (this.verificationFrequency == null) this.verificationFrequency = "DAILY";
    }

    @Builder
    private Challenge(String title, String description, User host,
                      int durationDays, LocalDate startDate, LocalDate endDate,
                      Long depositAmount, Integer maxParticipants,
                      int completionThresholdPct, ChallengeVisibility visibility,
                      String inviteCode) {
        this.title = title;
        this.description = description;
        this.host = host;
        this.durationDays = durationDays;
        this.startDate = startDate;
        this.endDate = endDate;
        this.depositAmount = depositAmount;
        this.maxParticipants = maxParticipants;
        this.completionThresholdPct = completionThresholdPct;
        this.visibility = visibility;
        this.inviteCode = inviteCode;
        this.verificationFrequency = "DAILY";
    }

    /** 방장이 시작 버튼을 누르면 즉시 IN_PROGRESS 전환 + 기간 확정 */
    public void start() {
        this.startDate = LocalDate.now();
        this.endDate = this.startDate.plusDays(this.durationDays - 1);
        this.status = ChallengeStatus.IN_PROGRESS;
    }

    /** 방장을 다른 참가자에게 위임한다 */
    public void delegateHost(User newHost) {
        this.host = newHost;
    }

    /** 챌린지 상태를 변경한다 (스케줄러 또는 전원 동의 취소 시 호출) */
    public void updateStatus(ChallengeStatus status) {
        this.status = status;
    }
}
