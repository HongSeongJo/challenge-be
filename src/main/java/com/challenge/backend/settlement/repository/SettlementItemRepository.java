package com.challenge.backend.settlement.repository;

import com.challenge.backend.settlement.entity.ChallengeSettlement;
import com.challenge.backend.settlement.entity.SettlementItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {

    List<SettlementItem> findBySettlement(ChallengeSettlement settlement);
}
