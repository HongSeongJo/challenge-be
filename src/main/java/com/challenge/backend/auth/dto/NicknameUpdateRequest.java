package com.challenge.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(@NotBlank @Size(max = 30) String nickname) {
}
