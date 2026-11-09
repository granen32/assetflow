package com.assetflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Controller의 @Valid로 입력 형식을 검사한다. 이메일 중복 같은 DB 의존 규칙은 Service가 처리한다. */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 80) String nickname,
        @NotBlank @Size(min = 8, max = 72) String password) {
    // record가 자동 생성하는 toString은 필드 값을 담으므로 비밀번호가 로그에 남지 않게 덮어쓴다.
    @Override public String toString() { return "RegisterRequest[REDACTED]"; }
}
