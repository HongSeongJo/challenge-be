package com.challenge.backend.user.dto;

import com.challenge.backend.user.entity.Role;
import com.challenge.backend.user.entity.User;

public record UserResponse(Long id, String email, String nickname, Role role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), user.getRole());
    }
}
