package com.challenge.backend.challenge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotHostException extends RuntimeException {
    public NotHostException() {
        super("방장만 수행할 수 있습니다.");
    }
}
