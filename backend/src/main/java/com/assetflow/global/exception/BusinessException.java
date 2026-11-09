package com.assetflow.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 예상 가능한 업무 실패를 상태 코드와 함께 전달한다. RuntimeException이므로
 * 기본 @Transactional 규칙에서 롤백 대상이며 HTTP 응답 변환은 ApiExceptionHandler가 맡는다.
 */
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
