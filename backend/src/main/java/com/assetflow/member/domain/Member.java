package com.assetflow.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Entity는 DB에 저장할 상태를 표현한다. API 요청/응답에는 직접 사용하지 않는다. */
@Entity
@Table(name = "members")
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    // DB의 UNIQUE 제약이 동시 가입 경쟁도 막는다. existsByEmail 사전 검사만으로는 부족하다.
    private String email;

    @Column(nullable = false, length = 80)
    private String nickname;

    @Column(length = 100)
    // 인증 도입 전 회원을 보존하기 위해 null을 허용한다. null인 회원의 로그인은 AuthService가 거절한다.
    private String passwordHash;

    @Column(nullable = false)
    private Instant createdAt;

    protected Member() {} // JPA가 엔티티를 복원할 때 필요한 생성자

    public Member(String email, String nickname) {
        // 기존 학습 데이터·테스트용 생성 경로다. 실제 신규 가입은 해시를 전달하는 생성자를 사용한다.
        this(email, nickname, null);
    }

    public Member(String email, String nickname, String passwordHash) {
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getPasswordHash() { return passwordHash; }
}
