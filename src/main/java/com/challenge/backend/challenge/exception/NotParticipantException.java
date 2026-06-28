package com.challenge.backend.challenge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotParticipantException extends RuntimeException {
    public NotParticipantException() {
        super("챌린지 참가자가 아닙니다.");
    }
}
