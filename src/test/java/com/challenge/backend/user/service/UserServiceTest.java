package com.challenge.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.challenge.backend.user.entity.ProviderType;
import com.challenge.backend.user.entity.Role;
import com.challenge.backend.user.entity.User;
import com.challenge.backend.user.repository.AuthProviderRepository;
import com.challenge.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthProviderRepository authProviderRepository;

    @Test
    void 신규_이메일로_카카오_로그인하면_새_User와_AuthProvider가_생성된다() {
        User user = userService.findOrCreateForKakao("kakao-1", "kakao-new@example.com");

        assertThat(user.getId()).isNotNull();
        assertThat(user.hasPassword()).isFalse();
        assertThat(user.isNicknameConfirmed()).isFalse();
        assertThat(authProviderRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, "kakao-1"))
                .isPresent();
    }

    @Test
    void 이미_가입된_이메일로_카카오_로그인하면_기존_User에_AuthProvider만_추가된다() {
        User existing = userRepository.save(new User("merge@example.com", "encoded-password", "기존유저", Role.USER, true));

        User result = userService.findOrCreateForKakao("kakao-2", "merge@example.com");

        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(userRepository.findByEmail("merge@example.com")).hasValueSatisfying(
                u -> assertThat(u.getId()).isEqualTo(existing.getId()));
    }

    @Test
    void 동일한_카카오_회원번호로_다시_로그인하면_같은_User가_반환된다() {
        User first = userService.findOrCreateForKakao("kakao-3", "repeat@example.com");
        User second = userService.findOrCreateForKakao("kakao-3", "repeat@example.com");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(authProviderRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, "kakao-3"))
                .hasValueSatisfying(p -> assertThat(p.getUser().getId()).isEqualTo(first.getId()));
    }

    @Test
    void 닉네임을_설정하면_nicknameConfirmed가_true가_된다() {
        User user = userService.findOrCreateForKakao("kakao-4", "set-nickname@example.com");

        User updated = userService.setNickname(user.getId(), "새닉네임");

        assertThat(updated.getNickname()).isEqualTo("새닉네임");
        assertThat(updated.isNicknameConfirmed()).isTrue();
    }
}
