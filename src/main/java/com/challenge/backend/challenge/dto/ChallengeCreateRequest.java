package com.challenge.backend.challenge.dto;

import com.challenge.backend.challenge.entity.ChallengeVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChallengeCreateRequest(

        @NotBlank @Size(max = 100)
        String title,

        @Size(max = 1000)
        String description,

        /** 챌린지 기간 (일). 방장이 시작 버튼을 누른 날부터 이 일수만큼 진행됨. */
        @NotNull @Min(1) @Max(365)
        Integer durationDays,

        @NotNull @Positive
        Long depositAmount,

        @Positive
        Integer maxParticipants,

        @NotNull @Min(1) @Max(100)
        Integer completionThresholdPct,

        @NotNull
        ChallengeVisibility visibility
) {}
