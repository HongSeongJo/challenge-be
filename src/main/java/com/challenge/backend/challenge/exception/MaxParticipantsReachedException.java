package com.challenge.backend.challenge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MaxParticipantsReachedException extends RuntimeException {
    public MaxParticipantsReachedException() {
        super("최대 참가 인원에 도달했습니다.");
    }
}
