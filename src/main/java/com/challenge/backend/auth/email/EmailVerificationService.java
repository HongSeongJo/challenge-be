package com.challenge.backend.auth.email;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 이메일 인증 코드를 발급/검증하고, 검증 성공 여부를 가입 직전까지 잠깐 들고 있는다.
 * User 테이블에 별도 emailVerified 컬럼을 두지 않는 이유: 가입(register) 자체를
 * "인증된 이메일만 통과"하는 게이트로 막아두면, 존재하는 모든 일반가입 유저는
 * 항상 이메일 소유권이 증명된 상태라는 게 구조적으로 보장되기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CODE_KEY_PREFIX = "auth:email-code:";
    private static final String VERIFIED_KEY_PREFIX = "auth:email-verified:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public String issueCode(String email) {
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, CODE_TTL);
        return code;
    }

    public boolean verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + email);
        if (savedCode == null || !savedCode.equals(code)) {
            return false;
        }
        redisTemplate.delete(CODE_KEY_PREFIX + email);
        redisTemplate.opsForValue().set(VERIFIED_KEY_PREFIX + email, "1", VERIFIED_TTL);
        return true;
    }

    public boolean isVerified(String email) {
        return redisTemplate.hasKey(VERIFIED_KEY_PREFIX + email);
    }

    public void consumeVerified(String email) {
        redisTemplate.delete(VERIFIED_KEY_PREFIX + email);
    }
}
