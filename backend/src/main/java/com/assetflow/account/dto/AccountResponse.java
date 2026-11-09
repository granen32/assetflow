package com.assetflow.account.dto;

import com.assetflow.account.domain.Account;
import com.assetflow.account.domain.AccountType;
import java.math.BigDecimal;

/** API 응답 구조를 Entity와 분리한다. DB 필드를 추가해도 자동으로 응답에 노출되지 않는다. */
public record AccountResponse(Long id, Long memberId, String name, AccountType type,
                              BigDecimal balance, String currency) {
    public static AccountResponse from(Account account) {
        // balance라는 응답 이름을 사용하지만 현재 값은 초기 잔액이다. 거래 합산은 미구현이다.
        return new AccountResponse(account.getId(), account.getMember().getId(),
                account.getName(), account.getType(), account.getOpeningBalance(), account.getCurrency());
    }
}
