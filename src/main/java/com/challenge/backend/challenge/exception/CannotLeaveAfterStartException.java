package com.challenge.backend.challenge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CannotLeaveAfterStartException extends RuntimeException {
    public CannotLeaveAfterStartException() {
        super("챌린지 시작 후에는 참가 취소가 불가합니다.");
    }
}
