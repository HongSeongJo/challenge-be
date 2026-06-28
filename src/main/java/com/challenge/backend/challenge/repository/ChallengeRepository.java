package com.challenge.backend.challenge.repository;

import com.challenge.backend.challenge.entity.Challenge;
import com.challenge.backend.challenge.entity.ChallengeStatus;
import com.challenge.backend.challenge.entity.ChallengeVisibility;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    Optional<Challenge> findByInviteCode(String inviteCode);

    /** 내가 방장인 챌린지 */
    List<Challenge> findByHostId(Long hostId);

    /** 공개 챌린지 목록 — 2.5단계 이후 활성화 */
    List<Challenge> findByVisibilityAndStatus(ChallengeVisibility visibility, ChallengeStatus status);

    /** 종료일이 지난 IN_PROGRESS 챌린지 조회 (4단계 스케줄러용) */
    @Query("SELECT c FROM Challenge c WHERE c.status = 'IN_PROGRESS' AND c.endDate < :today")
    List<Challenge> findChallengesToComplete(@Param("today") LocalDate today);
}
