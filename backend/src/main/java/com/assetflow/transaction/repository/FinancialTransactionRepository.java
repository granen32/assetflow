package com.assetflow.transaction.repository;

import com.assetflow.transaction.domain.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    Page<FinancialTransaction> findByAccountIdAndAccountMemberIdOrderByOccurredAtDescIdDesc(
            Long accountId,
            Long memberId,
            Pageable pageable);
}
