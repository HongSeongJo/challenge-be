package com.challenge.backend.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidEmailCodeException extends RuntimeException {
    public InvalidEmailCodeException() {
        super("인증 코드가 올바르지 않거나 만료되었습니다.");
    }
}
