package com.assetflow.account.dto;

import com.assetflow.account.domain.Account;
import com.assetflow.account.domain.AccountType;
import java.math.BigDecimal;

public record AccountResponse(Long id, Long memberId, String name, AccountType type,
                              BigDecimal balance, String currency) {
    public static AccountResponse from(Account account) {
        // 거래 기능 이전에는 초기 잔액이 현재 잔액이다. 2주차에 거래 합산으로 확장한다.
        return new AccountResponse(account.getId(), account.getMember().getId(),
                account.getName(), account.getType(), account.getOpeningBalance(), account.getCurrency());
    }
}
