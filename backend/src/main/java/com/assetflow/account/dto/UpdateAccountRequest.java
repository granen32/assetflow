package com.assetflow.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 이름만 변경할 수 있다. 잔액이나 소유자를 같은 PATCH로 바꾸지 못하도록 입력 범위를 제한한다.
public record UpdateAccountRequest(@NotBlank @Size(max = 100) String name) {}
