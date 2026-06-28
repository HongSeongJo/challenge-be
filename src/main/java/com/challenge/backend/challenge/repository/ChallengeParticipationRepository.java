package com.challenge.backend.challenge.repository;

import com.challenge.backend.challenge.entity.Challenge;
import com.challenge.backend.challenge.entity.ChallengeParticipation;
import com.challenge.backend.challenge.entity.ParticipationStatus;
import com.challenge.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChallengeParticipationRepository extends JpaRepository<ChallengeParticipation, Long> {

    Optional<ChallengeParticipation> findByChallengeAndUser(Challenge challenge, User user);

    boolean existsByChallengeAndUser(Challenge challenge, User user);

    List<ChallengeParticipation> findByChallenge(Challenge challenge);

    /** 내가 참가한 챌린지 목록 */
    @Query("SELECT p FROM ChallengeParticipation p JOIN FETCH p.challenge WHERE p.user.id = :userId")
    List<ChallengeParticipation> findByUserIdWithChallenge(@Param("userId") Long userId);

    long countByChallenge(Challenge challenge);

    long countByChallengeAndStatus(Challenge challenge, ParticipationStatus status);

    /** 전원 취소 투표 여부 확인 */
    @Query("SELECT COUNT(p) = COUNT(CASE WHEN p.cancelVoted = true THEN 1 END) FROM ChallengeParticipation p WHERE p.challenge = :challenge AND p.status = 'ACTIVE'")
    boolean allActiveParticipantsVotedCancel(@Param("challenge") Challenge challenge);
}
