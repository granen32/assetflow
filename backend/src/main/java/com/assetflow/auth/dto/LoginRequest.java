package com.assetflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 로그인은 기존 비밀번호를 확인하는 요청이다. 가입 시의 최소 길이 정책은 RegisterRequest에 둔다.
public record LoginRequest(@NotBlank @Email @Size(max = 255) String email,
                           @NotBlank @Size(max = 72) String password) {
    // DTO를 실수로 로그에 출력하더라도 비밀번호 값이 드러나지 않도록 한다.
    @Override public String toString() { return "LoginRequest[REDACTED]"; }
}
