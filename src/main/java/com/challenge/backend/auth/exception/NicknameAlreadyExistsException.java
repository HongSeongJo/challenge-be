package com.challenge.backend.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class NicknameAlreadyExistsException extends RuntimeException {
    public NicknameAlreadyExistsException(String nickname) {
        super("이미 사용 중인 닉네임입니다: " + nickname);
    }
}
