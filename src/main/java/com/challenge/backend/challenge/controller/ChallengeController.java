package com.challenge.backend.challenge.controller;

import com.challenge.backend.auth.security.AuthenticatedUser;
import com.challenge.backend.challenge.dto.ChallengeCreateRequest;
import com.challenge.backend.challenge.dto.ChallengeResponse;
import com.challenge.backend.challenge.dto.HostDelegateRequest;
import com.challenge.backend.challenge.dto.InviteInfoResponse;
import com.challenge.backend.challenge.dto.PaymentConfirmRequest;
import com.challenge.backend.challenge.service.ChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Challenge", description = "챌린지 생성 · 참가 · 조회 · 방장 위임 · 취소 투표 API")
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @Operation(summary = "챌린지 생성",
            description = "새 챌린지를 만든다. 생성자가 자동으로 방장이 되며 첫 번째 참가자로 등록된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 날짜 또는 입력값")
    })
    @PostMapping
    public ResponseEntity<ChallengeResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ChallengeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(challengeService.create(principal.userId(), request));
    }

    @Operation(summary = "내 챌린지 목록 조회",
            description = "내가 참가한 챌린지 전체 목록을 반환한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me")
    public List<ChallengeResponse> getMyParticipations(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return challengeService.getMyParticipations(principal.userId());
    }

    @Operation(summary = "챌린지 상세 조회",
            description = "챌린지 ID로 상세 정보를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "챌린지 없음")
    })
    @GetMapping("/{id}")
    public ChallengeResponse getDetail(@PathVariable Long id) {
        return challengeService.getDetail(id);
    }

    @Operation(summary = "초대 코드로 챌린지 미리보기",
            description = "참가 전 초대 코드로 챌린지 정보를 확인한다. 인증 불필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드")
    })
    @GetMapping("/invite/{inviteCode}")
    public InviteInfoResponse getByInviteCode(@PathVariable String inviteCode) {
        return challengeService.getByInviteCode(inviteCode);
    }

    @Operation(summary = "초대 코드로 챌린지 참가",
            description = "초대 코드로 챌린지에 참가한다. 보증금 결제(PENDING) 기록이 생성되며, 방장의 수동 확인 후 PAID 전환.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참가 신청 성공"),
            @ApiResponse(responseCode = "400", description = "모집 중 상태가 아님"),
            @ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드"),
            @ApiResponse(responseCode = "409", description = "이미 참가 중 또는 정원 초과")
    })
    @PostMapping("/invite/{inviteCode}/join")
    public ChallengeResponse join(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String inviteCode) {
        return challengeService.join(principal.userId(), inviteCode);
    }

    @Operation(summary = "보증금 입금 수동 확인 (방장 전용)",
            description = "MANUAL 방식에서 방장이 참가자의 입금을 확인하고 PAID 처리한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "확인 완료"),
            @ApiResponse(responseCode = "403", description = "방장이 아님"),
            @ApiResponse(responseCode = "404", description = "참가 기록 없음")
    })
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<Void> confirmPayment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody PaymentConfirmRequest request) {
        challengeService.confirmPayment(principal.userId(), id, request.participationId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "참가 취소",
            description = "챌린지 시작 전(RECRUITING)에만 취소 가능. MANUAL 방식은 방장이 직접 환불해야 한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "이미 시작된 챌린지"),
            @ApiResponse(responseCode = "403", description = "참가자가 아님")
    })
    @DeleteMapping("/{id}/leave")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id) {
        challengeService.leave(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "방장 위임",
            description = "현재 방장이 다른 참가자에게 방장 권한을 넘긴다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "위임 성공"),
            @ApiResponse(responseCode = "403", description = "방장이 아님"),
            @ApiResponse(responseCode = "404", description = "대상 사용자가 참가자가 아님")
    })
    @PatchMapping("/{id}/host")
    public ResponseEntity<Void> delegateHost(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody HostDelegateRequest request) {
        challengeService.delegateHost(principal.userId(), id, request.newHostUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "챌린지 시작 (방장 전용)",
            description = "방장이 시작 버튼을 누르면 즉시 IN_PROGRESS로 전환된다. 시작일은 오늘, 종료일은 오늘 + durationDays - 1.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "시작 완료"),
            @ApiResponse(responseCode = "400", description = "이미 시작됐거나 모집 중이 아님"),
            @ApiResponse(responseCode = "403", description = "방장이 아님"),
            @ApiResponse(responseCode = "404", description = "챌린지 없음")
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<Void> start(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id) {
        challengeService.startChallenge(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "전원 동의 즉시 취소 투표",
            description = "진행 중 챌린지를 취소하는 데 동의한다. 모든 참가자가 동의하면 즉시 CANCELLED + 전액 환불 처리.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "투표 완료"),
            @ApiResponse(responseCode = "400", description = "진행 중 상태가 아님"),
            @ApiResponse(responseCode = "403", description = "참가자가 아님")
    })
    @PostMapping("/{id}/vote-cancel")
    public ResponseEntity<Void> voteCancel(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id) {
        challengeService.voteCancellation(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
