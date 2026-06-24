package com.challenge.backend.user.service;

import com.challenge.backend.user.entity.AuthProvider;
import com.challenge.backend.user.entity.ProviderType;
import com.challenge.backend.user.entity.Role;
import com.challenge.backend.user.entity.User;
import com.challenge.backend.user.repository.AuthProviderRepository;
import com.challenge.backend.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    /**
     * 동일 이메일로 카카오 로그인이 들어오면 신규 User를 만들지 않고 기존 row에 AuthProvider(KAKAO)만 추가한다(계정 통합).
     * 이미 같은 카카오 회원번호로 가입된 적이 있으면 그 User를 그대로 반환한다.
     */
    @Transactional
    public User findOrCreateForKakao(String providerUserId, String email, String nickname) {
        var existingProvider = authProviderRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, providerUserId);
        if (existingProvider.isPresent()) {
            return existingProvider.get().getUser();
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User(email, null, nickname, Role.USER)));

        authProviderRepository.save(new AuthProvider(user, ProviderType.KAKAO, providerUserId));
        return user;
    }
}
