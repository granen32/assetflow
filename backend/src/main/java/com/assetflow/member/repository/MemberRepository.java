package com.assetflow.member.repository;

import com.assetflow.member.domain.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** JpaRepository가 save, findById 등 기본 CRUD 구현을 제공한다. */
public interface MemberRepository extends JpaRepository<Member, Long> {
    // boolean은 가입 중복 검사, Optional은 로그인 조회에서 회원이 없을 수 있음을 표현한다.
    boolean existsByEmail(String email);
    Optional<Member> findByEmail(String email);
}
