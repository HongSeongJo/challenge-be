package com.challenge.backend.challenge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ChallengeNotFoundException extends RuntimeException {
    public ChallengeNotFoundException() {
        super("챌린지를 찾을 수 없습니다.");
    }
}
