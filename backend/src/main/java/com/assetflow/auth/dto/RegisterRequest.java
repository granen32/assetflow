package com.assetflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 80) String nickname,
        @NotBlank @Size(min = 8, max = 72) String password) {
    @Override public String toString() { return "RegisterRequest[REDACTED]"; }
}
