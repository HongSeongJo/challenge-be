package com.challenge.backend.settlement.repository;

import com.challenge.backend.challenge.entity.Challenge;
import com.challenge.backend.settlement.entity.ChallengeSettlement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeSettlementRepository extends JpaRepository<ChallengeSettlement, Long> {

    Optional<ChallengeSettlement> findByChallenge(Challenge challenge);

    boolean existsByChallenge(Challenge challenge);
}
