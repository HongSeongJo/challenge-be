package com.challenge.backend.settlement.dto;

import com.challenge.backend.settlement.entity.ChallengeSettlement;
import com.challenge.backend.settlement.entity.SettlementItem;
import com.challenge.backend.settlement.entity.SettlementType;
import java.time.LocalDateTime;
import java.util.List;

/** 챌린지 정산 결과 응답 — 요약 정보 + 참가자별 지급 내역 */
public record SettlementResponse(
        /** 정산 방식 (NORMAL: 보상 분배 / FULL_REFUND: 전액 환불) */
        SettlementType type,
        /** 전체 보증금 풀 (원 단위) */
        Long totalPool,
        /** 완주자 수 */
        int completedCount,
        /** 미완주자 수 */
        int failedCount,
        /** 1인당 수령 금액 */
        Long amountPerCompleter,
        /** 정산 처리 일시 */
        LocalDateTime settledAt,
        /** 참가자별 개별 지급 내역 */
        List<ItemResponse> items
) {
    /** 참가자 1명의 정산 내역 */
    public record ItemResponse(
            Long userId,
            String nickname,
            Long receivedAmount,
            String type
    ) {}

    public static SettlementResponse of(ChallengeSettlement settlement, List<SettlementItem> items) {
        return new SettlementResponse(
                settlement.getType(),
                settlement.getTotalPool(),
                settlement.getCompletedCount(),
                settlement.getFailedCount(),
                settlement.getAmountPerCompleter(),
                settlement.getSettledAt(),
                items.stream()
                        .map(i -> new ItemResponse(
                                i.getUser().getId(),
                                i.getUser().getNickname(),
                                i.getReceivedAmount(),
                                i.getType().name()))
                        .toList()
        );
    }
}
