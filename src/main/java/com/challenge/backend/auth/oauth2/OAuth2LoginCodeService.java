package com.challenge.backend.auth.oauth2;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 카카오 로그인 성공 시 실제 JWT를 URL에 노출하지 않기 위해, 1회용 임시 코드를 발급/소비한다.
 * 코드는 Redis에 짧은 TTL로 저장되고, 교환(consume) 즉시 삭제되어 재사용할 수 없다.
 */
@Service
@RequiredArgsConstructor
public class OAuth2LoginCodeService {

    private static final String KEY_PREFIX = "oauth2:login-code:";
    private static final Duration CODE_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    public String issueCode(Long userId) {
        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + code, String.valueOf(userId), CODE_TTL);
        return code;
    }

    public Long consumeCode(String code) {
        String userId = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);
        return userId == null ? null : Long.valueOf(userId);
    }
}
