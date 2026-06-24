package com.challenge.backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이메일이 계정 통합 키 역할을 한다. 동일 이메일로 카카오 로그인이 들어오면
 * 신규 User를 만들지 않고 이 row에 AuthProvider(KAKAO)만 추가한다(자동 계정 통합).
 * 분리 운영 정책으로 바꾸려면 user.service.UserService#findOrCreateForKakao 의
 * "이메일 일치 시 통합" 분기만 "이미 가입된 이메일입니다" 예외로 교체하면 된다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // 소셜 전용 가입자는 비밀번호가 없을 수 있다.
    @Column(name = "password")
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    // 일반 가입은 가입 시점에 닉네임을 직접 입력하므로 항상 true.
    // 카카오 최초 가입은 임시 닉네임으로 생성되므로 false -> FE에서 닉네임 설정 화면으로 보내야 한다.
    @Column(name = "nickname_confirmed", nullable = false)
    private boolean nicknameConfirmed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public User(String email, String password, String nickname, Role role, boolean nicknameConfirmed) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.nicknameConfirmed = nicknameConfirmed;
        this.createdAt = Instant.now();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
        this.nicknameConfirmed = true;
    }

    public boolean hasPassword() {
        return this.password != null;
    }
}
