package com.challenge.backend.auth.jwt;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 로그아웃된 refresh token을 jti(JWT ID) 기준으로 블랙리스트에 올린다.
 * TTL을 토큰의 남은 만료 시간으로 맞춰서, 토큰이 자연 만료되면 Redis에서도 자동으로 사라지게 한다.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenBlacklistService {

    private static final String KEY_PREFIX = "auth:blacklist:refresh:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "1", ttl);
    }

    public boolean isBlacklisted(String tokenId) {
        return redisTemplate.hasKey(KEY_PREFIX + tokenId);
    }
}
