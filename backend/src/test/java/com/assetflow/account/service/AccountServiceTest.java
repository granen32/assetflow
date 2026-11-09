package com.assetflow.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.assetflow.account.domain.Account;
import com.assetflow.account.domain.AccountType;
import com.assetflow.account.dto.CreateAccountRequest;
import com.assetflow.account.repository.AccountRepository;
import com.assetflow.global.exception.BusinessException;
import com.assetflow.member.domain.Member;
import com.assetflow.member.repository.MemberRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock AccountRepository accounts;
    @Mock MemberRepository members;
    @InjectMocks AccountService service;

    @Test
    void createsAccountForExistingMember() {
        Member member = new Member("dev@example.com", "개발자");
        when(members.findById(1L)).thenReturn(Optional.of(member));
        when(accounts.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // DB 저장 전에는 자동 생성 ID가 null이지만 서비스의 매핑/로직을 검증할 수 있다.
        var result = service.create(1L, new CreateAccountRequest("생활비", AccountType.CHECKING,
                new BigDecimal("250000.00"), "KRW"));
        assertThat(result.name()).isEqualTo("생활비");
        assertThat(result.balance()).isEqualByComparingTo("250000.00");
    }

    @Test
    void rejectsUnknownMember() {
        when(members.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(99L, new CreateAccountRequest("생활비",
                AccountType.CASH, BigDecimal.ZERO, "KRW")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("회원을 찾을 수 없습니다.");
    }
}
