package com.challenge.backend.challenge.dto;

import jakarta.validation.constraints.NotNull;

/** 방장이 특정 참가자의 입금을 수동 확인할 때 사용 */
public record PaymentConfirmRequest(@NotNull Long participationId) {}
