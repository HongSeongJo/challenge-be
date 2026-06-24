package com.challenge.backend.auth.security;

import com.challenge.backend.user.entity.Role;

/**
 * JWT에서 추출해 SecurityContext의 principal로 채워 넣는 값.
 * DB 조회 없이 매 요청 인가를 처리하기 위해 userId/role만 들고 다닌다.
 */
public record AuthenticatedUser(Long userId, String email, Role role) {
}
