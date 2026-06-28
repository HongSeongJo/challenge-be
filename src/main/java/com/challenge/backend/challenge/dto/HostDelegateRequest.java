package com.challenge.backend.challenge.dto;

import jakarta.validation.constraints.NotNull;

public record HostDelegateRequest(@NotNull Long newHostUserId) {}
