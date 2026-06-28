package com.challenge.backend.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.challenge.backend.challenge.entity.Challenge;
import com.challenge.backend.challenge.entity.ChallengeVisibility;
import com.challenge.backend.user.entity.User;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.challenge.backend.challenge.repository.ChallengeParticipationRepository;
import com.challenge.backend.payment.repository.PaymentRepository;
import com.challenge.backend.settlement.repository.ChallengeSettlementRepository;
import com.challenge.backend.settlement.repository.SettlementItemRepository;
import com.challenge.backend.verification.service.VerificationQueryService;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementService 금액 계산 단위 테스트")
class SettlementServiceTest {

    @Mock ChallengeParticipationRepository participationRepository;
    @Mock ChallengeSettlementRepository settlementRepository;
    @Mock SettlementItemRepository settlementItemRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock VerificationQueryService verificationQueryService;

    @InjectMocks
    SettlementService settlementService;

    private Challenge challenge;

    @BeforeEach
    void setUp() {
        User host = new User("host@test.com", "pw", "방장",
                com.challenge.backend.user.entity.Role.USER, true);

        challenge = Challenge.builder()
                .title("테스트 챌린지")
                .host(host)
                .durationDays(30)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 30))
                .depositAmount(10_000L)
                .completionThresholdPct(80)
                .visibility(ChallengeVisibility.PRIVATE)
                .inviteCode("TEST123456AB")
                .build();
    }

    // ── calculateTotalDays ────────────────────────────────────

    @Test
    @DisplayName("30일 챌린지의 전체 인증 가능 일수는 30일")
    void totalDays_30dayChallenge() {
        assertThat(settlementService.calculateTotalDays(challenge)).isEqualTo(30);
    }

    @Test
    @DisplayName("시작일과 종료일이 같으면 1일")
    void totalDays_singleDay() {
        Challenge oneDay = Challenge.builder()
                .title("1일")
                .host(challenge.getHost())
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 1))
                .depositAmount(10_000L)
                .completionThresholdPct(100)
                .visibility(ChallengeVisibility.PRIVATE)
                .inviteCode("ONEDAY1234AB")
                .build();

        assertThat(settlementService.calculateTotalDays(oneDay)).isEqualTo(1);
    }

    // ── calculateRequired ─────────────────────────────────────

    @Test
    @DisplayName("30일 챌린지 80% 기준 → 최소 24일 인증 필요")
    void required_30days_80pct() {
        assertThat(settlementService.calculateRequired(30, 80)).isEqualTo(24);
    }

    @Test
    @DisplayName("30일 챌린지 100% 기준 → 30일 전부 인증 필요")
    void required_30days_100pct() {
        assertThat(settlementService.calculateRequired(30, 100)).isEqualTo(30);
    }

    @Test
    @DisplayName("7일 챌린지 70% 기준 → 올림 적용해서 5일")
    void required_7days_70pct_ceiling() {
        // 7 * 0.7 = 4.9 → 올림 → 5
        assertThat(settlementService.calculateRequired(7, 70)).isEqualTo(5);
    }

    @Test
    @DisplayName("1일 챌린지 50% 기준 → 1일 (올림)")
    void required_1day_50pct() {
        // 1 * 0.5 = 0.5 → 올림 → 1
        assertThat(settlementService.calculateRequired(1, 50)).isEqualTo(1);
    }

    // ── calculateReward ───────────────────────────────────────

    @Test
    @DisplayName("10명 × 10,000원 풀에서 완주자 5명 → 1인당 20,000원")
    void reward_half_completed() {
        long totalPool = 10 * 10_000L;
        assertThat(settlementService.calculateReward(totalPool, 5, 0.0)).isEqualTo(20_000L);
    }

    @Test
    @DisplayName("10명 × 10,000원 풀에서 완주자 1명 → 1인당 100,000원")
    void reward_one_completed() {
        long totalPool = 10 * 10_000L;
        assertThat(settlementService.calculateReward(totalPool, 1, 0.0)).isEqualTo(100_000L);
    }

    @Test
    @DisplayName("나눗셈 나머지는 버림 — 10,001원 풀 ÷ 완주자 3명 → 3,333원")
    void reward_floor_division() {
        // 10,001 / 3 = 3333.666... → 버림 → 3,333
        assertThat(settlementService.calculateReward(10_001L, 3, 0.0)).isEqualTo(3_333L);
    }

    @Test
    @DisplayName("수수료 10% 적용 시 순수풀은 90% — 100,000원 풀 ÷ 완주자 2명 → 45,000원")
    void reward_with_fee() {
        // 100,000 * 0.9 / 2 = 45,000
        assertThat(settlementService.calculateReward(100_000L, 2, 0.1)).isEqualTo(45_000L);
    }

    @Test
    @DisplayName("수수료 0%일 때 — 100,000원 풀 ÷ 완주자 2명 → 50,000원")
    void reward_no_fee() {
        assertThat(settlementService.calculateReward(100_000L, 2, 0.0)).isEqualTo(50_000L);
    }
}
