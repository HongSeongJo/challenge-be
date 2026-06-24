package com.challenge.backend.user.service;

import com.challenge.backend.user.entity.User;
import com.challenge.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    public java.util.List<User> getAll() {
        return userRepository.findAll();
    }
}
