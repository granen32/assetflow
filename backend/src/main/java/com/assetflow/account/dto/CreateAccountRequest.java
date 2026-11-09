package com.assetflow.account.dto;

import com.assetflow.account.domain.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 허용할 입력만 명시한다. 소유자 memberId는 인증 정보에서 가져오므로 요청 필드에 두지 않는다. */
public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull AccountType type,
        // DB NUMERIC(19,2) 범위를 요청 단계에서 검사해 저장 시 반올림·범위 초과를 줄인다.
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal openingBalance,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency
) {}
