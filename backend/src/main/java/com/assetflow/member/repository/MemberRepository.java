package com.assetflow.member.repository;

import com.assetflow.member.domain.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** JpaRepository가 save, findById 등 기본 CRUD 구현을 제공한다. */
public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
    Optional<Member> findByEmail(String email);
}
