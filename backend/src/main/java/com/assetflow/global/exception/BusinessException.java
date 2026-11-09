package com.assetflow.global.exception;

import org.springframework.http.HttpStatus;

/** 예상 가능한 비즈니스 오류를 HTTP 상태와 함께 표현한다. */
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
