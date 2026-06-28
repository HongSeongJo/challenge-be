package com.challenge.backend.challenge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ChallengeNotRecruitingException extends RuntimeException {
    public ChallengeNotRecruitingException() {
        super("모집 중인 챌린지가 아닙니다.");
    }
}
