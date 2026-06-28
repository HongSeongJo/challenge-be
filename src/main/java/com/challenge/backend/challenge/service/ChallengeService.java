package com.challenge.backend.challenge.service;

import com.challenge.backend.challenge.dto.ChallengeCreateRequest;
import com.challenge.backend.challenge.dto.ChallengeResponse;
import com.challenge.backend.challenge.dto.InviteInfoResponse;
import com.challenge.backend.challenge.entity.Challenge;
import com.challenge.backend.challenge.entity.ChallengeParticipation;
import com.challenge.backend.challenge.entity.ChallengeStatus;
import com.challenge.backend.challenge.exception.AlreadyJoinedException;
import com.challenge.backend.challenge.exception.CannotLeaveAfterStartException;
import com.challenge.backend.challenge.exception.ChallengeNotFoundException;
import com.challenge.backend.challenge.exception.ChallengeNotRecruitingException;
import com.challenge.backend.challenge.exception.MaxParticipantsReachedException;
import com.challenge.backend.challenge.exception.NotHostException;
import com.challenge.backend.challenge.exception.NotParticipantException;
import com.challenge.backend.challenge.repository.ChallengeParticipationRepository;
import com.challenge.backend.challenge.repository.ChallengeRepository;
import com.challenge.backend.payment.entity.Payment;
import com.challenge.backend.payment.entity.PaymentStatus;
import com.challenge.backend.payment.repository.PaymentRepository;
import com.challenge.backend.settlement.service.SettlementService;
import com.challenge.backend.user.entity.User;
import com.challenge.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipationRepository participationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SettlementService settlementService;

    /** 챌린지 생성 — 생성자가 자동으로 방장 + 첫 번째 참가자가 됨 */
    @Transactional
    public ChallengeResponse create(Long hostUserId, ChallengeCreateRequest req) {
        User host = getUser(hostUserId);

        Challenge challenge = Challenge.builder()
                .title(req.title())
                .description(req.description())
                .host(host)
                .durationDays(req.durationDays())
                .depositAmount(req.depositAmount())
                .maxParticipants(req.maxParticipants())
                .completionThresholdPct(req.completionThresholdPct())
                .visibility(req.visibility())
                .inviteCode(generateInviteCode())
                .build();
        challengeRepository.save(challenge);

        // 방장도 참가자로 등록 (보증금은 PENDING 상태로 시작)
        joinInternal(challenge, host);

        return ChallengeResponse.of(challenge, 1L);
    }

    /** 방장이 시작 버튼을 누르면 즉시 IN_PROGRESS 전환 */
    @Transactional
    public void startChallenge(Long hostUserId, Long challengeId) {
        Challenge challenge = getChallenge(challengeId);
        assertHost(challenge, hostUserId);

        if (challenge.getStatus() != ChallengeStatus.RECRUITING) {
            throw new ChallengeNotRecruitingException();
        }

        challenge.start();
    }

    /** 초대 코드로 참가 전 챌린지 정보 미리보기 */
    @Transactional(readOnly = true)
    public InviteInfoResponse getByInviteCode(String inviteCode) {
        Challenge challenge = challengeRepository.findByInviteCode(inviteCode)
                .orElseThrow(ChallengeNotFoundException::new);
        long count = participationRepository.countByChallenge(challenge);
        return InviteInfoResponse.of(challenge, count);
    }

    /** 초대 코드로 챌린지 참가 신청 (결제 PENDING 생성) */
    @Transactional
    public ChallengeResponse join(Long userId, String inviteCode) {
        User user = getUser(userId);
        Challenge challenge = challengeRepository.findByInviteCode(inviteCode)
                .orElseThrow(ChallengeNotFoundException::new);

        if (challenge.getStatus() != ChallengeStatus.RECRUITING) {
            throw new ChallengeNotRecruitingException();
        }
        if (participationRepository.existsByChallengeAndUser(challenge, user)) {
            throw new AlreadyJoinedException();
        }
        if (challenge.getMaxParticipants() != null) {
            long current = participationRepository.countByChallenge(challenge);
            if (current >= challenge.getMaxParticipants()) {
                throw new MaxParticipantsReachedException();
            }
        }

        joinInternal(challenge, user);

        long count = participationRepository.countByChallenge(challenge);
        return ChallengeResponse.of(challenge, count);
    }

    /** 방장이 특정 참가자의 보증금 입금을 수동으로 확인 */
    @Transactional
    public void confirmPayment(Long hostUserId, Long challengeId, Long participationId) {
        Challenge challenge = getChallenge(challengeId);
        assertHost(challenge, hostUserId);

        ChallengeParticipation participation = participationRepository.findById(participationId)
                .orElseThrow(NotParticipantException::new);
        Payment payment = paymentRepository.findByParticipation(participation)
                .orElseThrow(NotParticipantException::new);

        payment.confirmManual();
    }

    /** 내가 참가한 챌린지 목록 조회 */
    @Transactional(readOnly = true)
    public List<ChallengeResponse> getMyParticipations(Long userId) {
        return participationRepository.findByUserIdWithChallenge(userId).stream()
                .map(p -> {
                    long count = participationRepository.countByChallenge(p.getChallenge());
                    return ChallengeResponse.of(p.getChallenge(), count);
                })
                .toList();
    }

    /** 챌린지 상세 조회 */
    @Transactional(readOnly = true)
    public ChallengeResponse getDetail(Long challengeId) {
        Challenge challenge = getChallenge(challengeId);
        long count = participationRepository.countByChallenge(challenge);
        return ChallengeResponse.of(challenge, count);
    }

    /** 참가 취소 — 챌린지 시작(IN_PROGRESS) 전까지만 가능 */
    @Transactional
    public void leave(Long userId, Long challengeId) {
        Challenge challenge = getChallenge(challengeId);
        if (challenge.getStatus() != ChallengeStatus.RECRUITING) {
            throw new CannotLeaveAfterStartException();
        }

        User user = getUser(userId);
        ChallengeParticipation participation = participationRepository
                .findByChallengeAndUser(challenge, user)
                .orElseThrow(NotParticipantException::new);

        // 결제 기록이 있으면 CANCELLED 처리 (MANUAL이므로 실제 환불은 방장이 직접)
        paymentRepository.findByParticipation(participation)
                .ifPresent(Payment::cancel);

        participationRepository.delete(participation);
    }

    /** 방장 위임 */
    @Transactional
    public void delegateHost(Long hostUserId, Long challengeId, Long newHostUserId) {
        Challenge challenge = getChallenge(challengeId);
        assertHost(challenge, hostUserId);

        User newHost = getUser(newHostUserId);
        if (!participationRepository.existsByChallengeAndUser(challenge, newHost)) {
            throw new NotParticipantException();
        }

        challenge.delegateHost(newHost);
    }

    /** 전원 동의 즉시 취소 투표 */
    @Transactional
    public void voteCancellation(Long userId, Long challengeId) {
        Challenge challenge = getChallenge(challengeId);
        if (challenge.getStatus() != ChallengeStatus.IN_PROGRESS) {
            throw new ChallengeNotRecruitingException();
        }

        User user = getUser(userId);
        ChallengeParticipation participation = participationRepository
                .findByChallengeAndUser(challenge, user)
                .orElseThrow(NotParticipantException::new);

        participation.voteCancellation();

        // 전원 투표 완료 시 챌린지 즉시 취소 + 정산 (전액 환불)
        if (participationRepository.allActiveParticipantsVotedCancel(challenge)) {
            challenge.updateStatus(ChallengeStatus.CANCELLED);
            settlementService.settleCancelled(challenge);
        }
    }

    // ── 스케줄러용 상태 전환 (4단계 스케줄러 구현 시 호출) ────────────

    /** 종료일이 지난 IN_PROGRESS 챌린지를 COMPLETED로 전환 후 정산 */
    @Transactional
    public void completeDueChallenges() {
        challengeRepository.findChallengesToComplete(LocalDate.now())
                .forEach(c -> {
                    c.updateStatus(ChallengeStatus.COMPLETED);
                    settlementService.settle(c);
                });
    }

    // ── 내부 유틸 ─────────────────────────────────────────────

    /** 참가 기록 + PENDING 결제 기록 동시 생성 */
    private void joinInternal(Challenge challenge, User user) {
        ChallengeParticipation participation = ChallengeParticipation.builder()
                .challenge(challenge)
                .user(user)
                .build();
        participationRepository.save(participation);

        Payment payment = Payment.manualBuilder()
                .participation(participation)
                .amount(challenge.getDepositAmount())
                .build();
        paymentRepository.save(payment);
    }

    private Challenge getChallenge(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(ChallengeNotFoundException::new);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    private void assertHost(Challenge challenge, Long userId) {
        if (!challenge.getHost().getId().equals(userId)) {
            throw new NotHostException();
        }
    }

    /** 12자리 랜덤 영숫자 초대 코드 생성 (UUID 기반, 대문자) */
    private String generateInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (challengeRepository.findByInviteCode(code).isPresent());
        return code;
    }
}
