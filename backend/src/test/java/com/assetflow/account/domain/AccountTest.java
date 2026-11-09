package com.assetflow.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.assetflow.member.domain.Member;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

// 순수 Java 객체의 불변 조건을 검증한다. JPA 매핑이나 HTTP 검증은 이 테스트의 범위가 아니다.
class AccountTest {
    @Test
    void createsAccountAndRenamesIt() {
        Account account = new Account(new Member("dev@example.com", "개발자"),
                "생활비", AccountType.CHECKING, new BigDecimal("1000.00"), "KRW");
        account.rename("주거래");
        assertThat(account.getName()).isEqualTo("주거래");
        assertThat(account.getOpeningBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void rejectsNegativeOpeningBalance() {
        assertThatThrownBy(() -> new Account(new Member("dev@example.com", "개발자"),
                "생활비", AccountType.CHECKING, new BigDecimal("-1"), "KRW"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
