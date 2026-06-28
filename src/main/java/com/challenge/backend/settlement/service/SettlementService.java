package com.challenge.backend.settlement.service;

import com.challenge.backend.challenge.entity.Challenge;
import com.challenge.backend.challenge.entity.ChallengeParticipation;
import com.challenge.backend.challenge.entity.ParticipationStatus;
import com.challenge.backend.challenge.repository.ChallengeParticipationRepository;
import com.challenge.backend.payment.entity.Payment;
import com.challenge.backend.payment.repository.PaymentRepository;
import com.challenge.backend.settlement.dto.SettlementResponse;
import com.challenge.backend.settlement.entity.ChallengeSettlement;
import com.challenge.backend.settlement.entity.SettlementItem;
import com.challenge.backend.settlement.entity.SettlementItemType;
import com.challenge.backend.settlement.entity.SettlementType;
import com.challenge.backend.settlement.repository.ChallengeSettlementRepository;
import com.challenge.backend.settlement.repository.SettlementItemRepository;
import com.challenge.backend.verification.service.VerificationQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 챌린지 종료 후 완주 판정 및 보증금 정산을 처리하는 서비스.
 * 금액 계산 정확성이 중요하므로 단위 테스트 필수 (SettlementServiceTest 참고).
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final ChallengeParticipationRepository participationRepository;
    private final ChallengeSettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;
    private final PaymentRepository paymentRepository;
    private final VerificationQueryService verificationQueryService;

    /**
     * 챌린지 정산을 실행한다. (챌린지 COMPLETED 전환 시 호출)
     *
     * 처리 순서:
     * 1. 결제 완료(PAID) 참가자만 대상으로 함
     * 2. 각 참가자의 인증 횟수 조회 → 완주 여부 판정
     * 3. 정산 타입 결정 (NORMAL / FULL_REFUND)
     * 4. 금액 계산
     * 5. ChallengeSettlement + SettlementItem 저장
     */
    @Transactional
    public SettlementResponse settle(Challenge challenge) {
        if (settlementRepository.existsByChallenge(challenge)) {
            // 이미 정산된 챌린지는 저장된 결과를 반환
            ChallengeSettlement existing = settlementRepository.findByChallenge(challenge).orElseThrow();
            List<SettlementItem> items = settlementItemRepository.findBySettlement(existing);
            return SettlementResponse.of(existing, items);
        }

        List<ChallengeParticipation> participations = participationRepository.findByChallenge(challenge);

        // 결제 완료 참가자만 정산 대상
        List<Payment> payments = paymentRepository.findByParticipationIn(participations);
        List<ChallengeParticipation> paidParticipations = participations.stream()
                .filter(p -> payments.stream()
                        .anyMatch(pay -> pay.getParticipation().getId().equals(p.getId())
                                && pay.getStatus().name().equals("PAID")))
                .toList();

        int totalDays = calculateTotalDays(challenge);

        // 완주 판정
        for (ChallengeParticipation p : paidParticipations) {
            int verifiedCount = verificationQueryService.countVerified(challenge, p.getUser());
            int required = calculateRequired(totalDays, challenge.getCompletionThresholdPct());
            if (verifiedCount >= required) {
                p.complete();
            } else {
                p.fail();
            }
        }

        long completedCount = paidParticipations.stream()
                .filter(p -> p.getStatus() == ParticipationStatus.COMPLETED).count();
        long failedCount = paidParticipations.stream()
                .filter(p -> p.getStatus() == ParticipationStatus.FAILED).count();
        long paidCount = paidParticipations.size();

        Long depositAmount = challenge.getDepositAmount();
        Long totalPool = depositAmount * paidCount;

        // 정산 타입 및 금액 계산
        SettlementType type;
        Long amountPerCompleter;

        if (completedCount == 0 || failedCount == 0) {
            // 전원 완주 or 전원 실패 → 원금 환불
            type = SettlementType.FULL_REFUND;
            amountPerCompleter = depositAmount;
        } else {
            // 일부 완주 → 미완주자 보증금을 완주자에게 N분배
            type = SettlementType.NORMAL;
            amountPerCompleter = calculateReward(totalPool, completedCount, getPlatformFeeRate());
        }

        ChallengeSettlement settlement = ChallengeSettlement.builder()
                .challenge(challenge)
                .type(type)
                .totalPool(totalPool)
                .completedCount((int) completedCount)
                .failedCount((int) failedCount)
                .amountPerCompleter(amountPerCompleter)
                .build();
        settlementRepository.save(settlement);

        // 참가자별 정산 항목 생성
        SettlementItemType itemType = (type == SettlementType.FULL_REFUND)
                ? SettlementItemType.FULL_REFUND
                : SettlementItemType.REWARD;

        List<SettlementItem> items = paidParticipations.stream()
                .filter(p -> type == SettlementType.FULL_REFUND
                        || p.getStatus() == ParticipationStatus.COMPLETED)
                .map(p -> SettlementItem.builder()
                        .settlement(settlement)
                        .user(p.getUser())
                        .participation(p)
                        .receivedAmount(amountPerCompleter)
                        .type(itemType)
                        .build())
                .toList();
        settlementItemRepository.saveAll(items);

        return SettlementResponse.of(settlement, items);
    }

    /**
     * 전원 동의 취소 정산 — 진행 중 챌린지가 취소될 때 즉시 전원 원금 환불.
     */
    @Transactional
    public SettlementResponse settleCancelled(Challenge challenge) {
        List<ChallengeParticipation> participations = participationRepository.findByChallenge(challenge);
        List<Payment> payments = paymentRepository.findByParticipationIn(participations);

        List<ChallengeParticipation> paidParticipations = participations.stream()
                .filter(p -> payments.stream()
                        .anyMatch(pay -> pay.getParticipation().getId().equals(p.getId())
                                && pay.getStatus().name().equals("PAID")))
                .toList();

        Long depositAmount = challenge.getDepositAmount();
        Long totalPool = depositAmount * paidParticipations.size();

        ChallengeSettlement settlement = ChallengeSettlement.builder()
                .challenge(challenge)
                .type(SettlementType.FULL_REFUND)
                .totalPool(totalPool)
                .completedCount(0)
                .failedCount(0)
                .amountPerCompleter(depositAmount)
                .build();
        settlementRepository.save(settlement);

        List<SettlementItem> items = paidParticipations.stream()
                .map(p -> SettlementItem.builder()
                        .settlement(settlement)
                        .user(p.getUser())
                        .participation(p)
                        .receivedAmount(depositAmount)
                        .type(SettlementItemType.FULL_REFUND)
                        .build())
                .toList();
        settlementItemRepository.saveAll(items);

        // 결제 환불 처리
        payments.stream()
                .filter(pay -> pay.getStatus().name().equals("PAID"))
                .forEach(Payment::refund);
        paymentRepository.saveAll(payments);

        return SettlementResponse.of(settlement, items);
    }

    // ── 계산 유틸 (단위 테스트 대상) ──────────────────────────

    /** 챌린지 전체 인증 가능 일수 계산 */
    public int calculateTotalDays(Challenge challenge) {
        return (int) (challenge.getEndDate().toEpochDay() - challenge.getStartDate().toEpochDay()) + 1;
    }

    /**
     * 완주에 필요한 최소 인증 횟수 계산.
     * @param totalDays           전체 인증 가능 일수
     * @param thresholdPct        완주 기준 달성률 (1~100)
     * @return 완주에 필요한 최소 인증 횟수 (올림 적용)
     */
    public int calculateRequired(int totalDays, int thresholdPct) {
        return (int) Math.ceil(totalDays * thresholdPct / 100.0);
    }

    /**
     * 완주자 1인당 보상 금액 계산.
     * @param totalPool       전체 보증금 풀
     * @param completedCount  완주자 수
     * @param feeRate         플랫폼 수수료율 (현재 0.0)
     * @return 완주자 1인당 수령액 (원 단위, 내림 적용 — 나머지 원단위 버림)
     */
    public long calculateReward(long totalPool, long completedCount, double feeRate) {
        long netPool = (long) (totalPool * (1.0 - feeRate));
        return netPool / completedCount;
    }

    private double getPlatformFeeRate() {
        return 0.0; // TODO: 사업자 등록 후 수수료 정책 도입 시 변경
    }
}
