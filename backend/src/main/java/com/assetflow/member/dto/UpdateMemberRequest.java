package com.assetflow.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 80, message = "닉네임은 최대 80자까지 가능합니다.")
        String nickname
) {}