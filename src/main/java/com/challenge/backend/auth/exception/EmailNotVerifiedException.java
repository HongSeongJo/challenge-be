package com.challenge.backend.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("이메일 인증을 완료해주세요.");
    }
}
