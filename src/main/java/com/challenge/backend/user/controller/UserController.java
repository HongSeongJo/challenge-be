package com.challenge.backend.user.controller;

import com.challenge.backend.auth.dto.NicknameUpdateRequest;
import com.challenge.backend.auth.security.AuthenticatedUser;
import com.challenge.backend.user.dto.UserResponse;
import com.challenge.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserResponse.from(userService.getById(principal.userId()));
    }

    @PatchMapping("/me/nickname")
    public UserResponse updateNickname(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody NicknameUpdateRequest request) {
        return UserResponse.from(userService.setNickname(principal.userId(), request.nickname()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal AuthenticatedUser principal) {
        userService.withdraw(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
