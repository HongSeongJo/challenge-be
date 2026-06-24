package com.challenge.backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 User가 여러 로그인 수단(LOCAL, KAKAO, ...)을 가질 수 있도록 분리한 테이블.
 * (provider, providerUserId) 조합이 유일한 로그인 수단을 식별한다.
 */
@Entity
@Table(
        name = "auth_providers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderType provider;

    // LOCAL의 경우 가입 시점의 이메일을 그대로 사용, KAKAO는 카카오 회원번호(id)
    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    public AuthProvider(User user, ProviderType provider, String providerUserId) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }
}
