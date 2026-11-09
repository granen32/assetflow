package com.assetflow.account.repository;

import com.assetflow.account.domain.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // 회원 ID를 조건에 포함해 다른 회원의 계좌가 조회되지 않게 한다.
    List<Account> findAllByMemberIdOrderByIdDesc(Long memberId);
    Optional<Account> findByIdAndMemberId(Long id, Long memberId);
}
