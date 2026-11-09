package com.assetflow.account.repository;

import com.assetflow.account.domain.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // Spring Data가 메서드 이름을 해석한다: member.id 조건 + id 내림차순 정렬.
    // 이 조건에 전달하는 회원 ID는 Controller가 검증된 JWT에서 가져온 값이어야 한다.
    List<Account> findAllByMemberIdOrderByIdDesc(Long memberId);
    Optional<Account> findByIdAndMemberId(Long id, Long memberId);
}
