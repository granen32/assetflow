package com.assetflow.transaction.domain;

import com.assetflow.account.domain.Account;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "financial_transactions")
public class FinancialTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
  
    // 여러 거래가 하나의 계좌에 연결된다.
    // LAZY로 설정해 거래를 조회할 때 계좌까지 항상 불러오지 않는다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    //enum 순서가 바뀌어도 DB 값이 변하지 않도록 STRING으로 저장한다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    //전체 19자리중 소수 2자리 실제 db정의는 flyway sql이 담당한다.
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

     // 거래가 실제 발생한 시각이다. 서버에 등록한 시각과 구분한다.
    @Column(nullable = false)
    private Instant occurredAt;

    @Column(length = 500)
    private String memo;

    // 같은 이체에서 생성된 출금·입금 두 거래가 동일한 값을 가진다.
    // 일반 수입·지출에서는 null이다.
    private UUID transferId;

    // 거래가 생성된 시각이다. 서버에 등록한 시각과 구분한다.
    @Column(nullable = false)
    private Instant createdAt;

    // JPA가 DB의 데이터를 객체로 복원할 때 사용한다.
    protected FinancialTransaction() {}

    public FinancialTransaction(Account account, TransactionType type, BigDecimal amount, Instant occurredAt, String memo, UUID transferId) {
        this.account = account;
        this.type = type;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.occurredAt = occurredAt;
        this.memo = memo;
        this.transferId = transferId;
        this.createdAt = Instant.now();
    }

}
