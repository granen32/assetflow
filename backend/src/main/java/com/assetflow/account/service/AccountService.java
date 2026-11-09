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

/** 계좌 생성·조회·변경의 유스케이스와 트랜잭션 경계를 담당한다. */
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
        // JWT가 유효해도 회원이 DB에서 사라졌을 수 있으므로 실제 회원 존재 여부를 확인한다.
        Member member = members.findById(memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        Account account = new Account(member, request.name(), request.type(),
                request.openingBalance(), request.currency());
        return AccountResponse.from(accounts.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(Long memberId) {
        // readOnly는 조회 의도를 나타내고 JPA 최적화에 쓰인다. DB 쓰기를 막는 권한 설정은 아니다.
        // 트랜잭션 안에서 Entity를 DTO로 변환해 HTTP 직렬화가 지연 로딩에 의존하지 않게 한다.
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
        // 조회한 엔티티는 영속성 컨텍스트가 관리한다. 변경 감지로 flush 시 UPDATE가 실행되므로
        // 이 메서드에서는 변경 후 repository.save를 다시 호출하지 않는다.
        account.rename(request.name());
        return AccountResponse.from(account);
    }

    private Account findOwnedAccount(Long memberId, Long accountId) {
        // 계좌 ID와 소유자 ID를 함께 조회한다. 타인 계좌와 없는 계좌는 같은 404로 처리한다.
        return accounts.findByIdAndMemberId(accountId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."));
    }
}
