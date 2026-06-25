package com.challenge.backend.user.service;

import com.challenge.backend.auth.exception.NicknameAlreadyExistsException;
import com.challenge.backend.auth.oauth2.KakaoUnlinkClient;
import com.challenge.backend.user.entity.AuthProvider;
import com.challenge.backend.user.entity.ProviderType;
import com.challenge.backend.user.entity.Role;
import com.challenge.backend.user.entity.User;
import com.challenge.backend.user.repository.AuthProviderRepository;
import com.challenge.backend.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final KakaoUnlinkClient kakaoUnlinkClient;

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    @Transactional
    public User setNickname(Long userId, String nickname) {
        if (!isNicknameAvailable(nickname)) {
            throw new NicknameAlreadyExistsException(nickname);
        }
        User user = getById(userId);
        user.changeNickname(nickname);
        return user;
    }

    /**
     * 회원탈퇴: 카카오로 가입한 연동이 있으면 카카오 쪽 연결도 끊고(KakaoUnlinkClient),
     * 로컬 데이터(AuthProvider, User)를 삭제한다.
     * 진행 중인 챌린지가 있을 때 탈퇴를 막는 정책은 2단계(챌린지 도메인) 이후에 여기에 조건을 추가하면 된다.
     */
    @Transactional
    public void withdraw(Long userId) {
        User user = getById(userId);
        List<AuthProvider> providers = authProviderRepository.findAllByUser_Id(userId);

        providers.stream()
                .filter(provider -> provider.getProvider() == ProviderType.KAKAO)
                .forEach(provider -> kakaoUnlinkClient.unlink(provider.getProviderUserId()));

        authProviderRepository.deleteAll(providers);
        userRepository.delete(user);
    }

    /**
     * 동일 이메일로 카카오 로그인이 들어오면 신규 User를 만들지 않고 기존 row에 AuthProvider(KAKAO)만 추가한다(계정 통합).
     * 이미 같은 카카오 회원번호로 가입된 적이 있으면 그 User를 그대로 반환한다.
     * 신규 가입 시 닉네임은 카카오 프로필 값을 쓰지 않고 임시값("kakao_회원번호")으로 만들며,
     * FE가 nicknameConfirmed=false를 보고 닉네임 설정 화면으로 안내해야 한다.
     */
    @Transactional
    public User findOrCreateForKakao(String providerUserId, String email) {
        var existingProvider = authProviderRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, providerUserId);
        if (existingProvider.isPresent()) {
            return existingProvider.get().getUser();
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        new User(email, null, temporaryNickname(providerUserId), Role.USER, false)));

        authProviderRepository.save(new AuthProvider(user, ProviderType.KAKAO, providerUserId));
        return user;
    }

    private String temporaryNickname(String providerUserId) {
        String base = "kakao_" + providerUserId;
        return isNicknameAvailable(base) ? base : base + "_" + UUID.randomUUID().toString().substring(0, 6);
    }
}
