package com.challenge.backend.challenge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlreadyJoinedException extends RuntimeException {
    public AlreadyJoinedException() {
        super("이미 참가한 챌린지입니다.");
    }
}
