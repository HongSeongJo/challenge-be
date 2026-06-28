package com.challenge.backend.challenge.dto;

import com.challenge.backend.challenge.entity.Challenge;
import com.challenge.backend.challenge.entity.ChallengeStatus;
import com.challenge.backend.challenge.entity.ChallengeVisibility;
import java.time.LocalDate;

public record ChallengeResponse(
        Long id,
        String title,
        String description,
        String hostNickname,
        int durationDays,
        LocalDate startDate,   // 시작 전이면 null
        LocalDate endDate,     // 시작 전이면 null
        Long depositAmount,
        Integer maxParticipants,
        int completionThresholdPct,
        ChallengeStatus status,
        ChallengeVisibility visibility,
        String inviteCode,
        long participantCount
) {
    public static ChallengeResponse of(Challenge c, long participantCount) {
        return new ChallengeResponse(
                c.getId(), c.getTitle(), c.getDescription(),
                c.getHost().getNickname(),
                c.getDurationDays(), c.getStartDate(), c.getEndDate(),
                c.getDepositAmount(), c.getMaxParticipants(),
                c.getCompletionThresholdPct(),
                c.getStatus(), c.getVisibility(),
                c.getInviteCode(), participantCount
        );
    }
}
