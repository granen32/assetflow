package com.assetflow.account.service;

import com.assetflow.account.domain.Account;
import com.assetflow.account.dto.AccountResponse;
import com.assetflow.account.dto.CreateAccountRequest;
import com.assetflow.account.dto.UpdateAccountRequest;
import com.assetflow.account.repository.AccountRepository;
import com.assetflow.global.exception.BusinessException;
import com.assetflow.member.domain.Member;
import com.assetflow.member.repository.MemberRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountRepository accounts;
    private final MemberRepository members;

    public AccountService(AccountRepository accounts, MemberRepository members) {
        this.accounts = accounts;
        this.members = members;
    }

    @Transactional
    public AccountResponse create(Long memberId, CreateAccountRequest request) {
        Member member = members.findById(memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        Account account = new Account(member, request.name(), request.type(),
                request.openingBalance(), request.currency());
        return AccountResponse.from(accounts.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(Long memberId) {
        return accounts.findAllByMemberIdOrderByIdDesc(memberId).stream()
                .map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long memberId, Long accountId) {
        return AccountResponse.from(findOwnedAccount(memberId, accountId));
    }

    @Transactional
    public AccountResponse rename(Long memberId, Long accountId, UpdateAccountRequest request) {
        Account account = findOwnedAccount(memberId, accountId);
        // JPA dirty checking: 트랜잭션 종료 시 변경된 엔티티가 자동으로 반영된다.
        account.rename(request.name());
        return AccountResponse.from(account);
    }

    private Account findOwnedAccount(Long memberId, Long accountId) {
        return accounts.findByIdAndMemberId(accountId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."));
    }
}
