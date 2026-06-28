package com.challenge.backend.payment.repository;

import com.challenge.backend.challenge.entity.ChallengeParticipation;
import com.challenge.backend.payment.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByParticipation(ChallengeParticipation participation);

    Optional<Payment> findByMerchantUid(String merchantUid);

    Optional<Payment> findByImpUid(String impUid);

    List<Payment> findByParticipationIn(List<ChallengeParticipation> participations);
}
