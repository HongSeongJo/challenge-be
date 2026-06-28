package com.challenge.backend.challenge.dto;

import com.challenge.backend.challenge.entity.Challenge;
import com.challenge.backend.challenge.entity.ChallengeStatus;
import java.time.LocalDate;

/** 초대 코드로 참가 전 확인하는 챌린지 공개 정보 */
public record InviteInfoResponse(
        Long id,
        String title,
        String description,
        String hostNickname,
        LocalDate startDate,
        LocalDate endDate,
        Long depositAmount,
        Integer maxParticipants,
        int completionThresholdPct,
        ChallengeStatus status,
        long participantCount
) {
    public static InviteInfoResponse of(Challenge c, long participantCount) {
        return new InviteInfoResponse(
                c.getId(), c.getTitle(), c.getDescription(),
                c.getHost().getNickname(),
                c.getStartDate(), c.getEndDate(),
                c.getDepositAmount(), c.getMaxParticipants(),
                c.getCompletionThresholdPct(),
                c.getStatus(), participantCount
        );
    }
}
