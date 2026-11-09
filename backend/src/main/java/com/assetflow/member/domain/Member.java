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
    private String email;

    @Column(nullable = false, length = 80)
    private String nickname;

    @Column(length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private Instant createdAt;

    protected Member() {} // JPA가 엔티티를 복원할 때 필요한 생성자

    public Member(String email, String nickname) {
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
