package com.assetflow.account.domain;

import com.assetflow.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: 계좌 조회 시 회원 객체까지 무조건 불러오지 않도록 설정한다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING) // enum 순서가 바뀌어도 DB 값이 변하지 않는다.
    @Column(nullable = false, length = 30)
    private AccountType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal openingBalance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Instant createdAt;

    protected Account() {}

    public Account(Member member, String name, AccountType type, BigDecimal openingBalance, String currency) {
        if (openingBalance.signum() < 0) throw new IllegalArgumentException("초기 잔액은 음수일 수 없습니다.");
        this.member = member;
        this.name = name;
        this.type = type;
        this.openingBalance = openingBalance;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

    /** 이름 변경은 setter 대신 도메인 메서드로 의도를 드러낸다. */
    public void rename(String newName) { this.name = newName; }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public String getName() { return name; }
    public AccountType getType() { return type; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public String getCurrency() { return currency; }
}
