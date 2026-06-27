package com.challenge.backend.user.controller;

import com.challenge.backend.auth.dto.NicknameUpdateRequest;
import com.challenge.backend.auth.security.AuthenticatedUser;
import com.challenge.backend.user.dto.UserResponse;
import com.challenge.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "User", description = "내 프로필 조회 · 닉네임 변경 · 회원 탈퇴 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "JWT로 인증된 사용자의 프로필 정보를 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserResponse.from(userService.getById(principal.userId()));
    }

    @Operation(summary = "닉네임 변경", description = "닉네임을 새 값으로 변경한다. 중복 닉네임은 거부.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공 — 갱신된 프로필 반환"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 닉네임"),
            @ApiResponse(responseCode = "409", description = "닉네임 중복")
    })
    @PatchMapping("/me/nickname")
    public UserResponse updateNickname(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody NicknameUpdateRequest request) {
        return UserResponse.from(userService.setNickname(principal.userId(), request.nickname()));
    }

    @Operation(summary = "회원 탈퇴", description = "계정을 삭제한다. 카카오 소셜 계정이면 카카오 unlink도 함께 처리.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal AuthenticatedUser principal) {
        userService.withdraw(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
