package com.challenge.backend.user.repository;

import com.challenge.backend.user.entity.AuthProvider;
import com.challenge.backend.user.entity.ProviderType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthProviderRepository extends JpaRepository<AuthProvider, Long> {
    Optional<AuthProvider> findByProviderAndProviderUserId(ProviderType provider, String providerUserId);

    boolean existsByUser_IdAndProvider(Long userId, ProviderType provider);
}
