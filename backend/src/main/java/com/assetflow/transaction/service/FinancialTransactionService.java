package com.assetflow.transaction.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.assetflow.account.domain.Account;
import com.assetflow.account.repository.AccountRepository;
import com.assetflow.global.exception.BusinessException;
import com.assetflow.transaction.dto.TransactionResponse;
import com.assetflow.transaction.repository.FinancialTransactionRepository;

@Service
public class FinancialTransactionService {

    private final AccountRepository accounts;
    private final FinancialTransactionRepository transactions;

    public FinancialTransactionService(
            AccountRepository accounts,
            FinancialTransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    private Account findOwnedAccount(Long memberId, Long accountId) {
        return accounts.findByIdAndMemberId(accountId, memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(
            Long memberId, Long accountId, Pageable pageable) {
        findOwnedAccount(memberId, accountId);
        return transactions
                .findByAccountIdAndAccountMemberIdOrderByOccurredAtDescIdDesc(
                        accountId, memberId, pageable)
                .map(TransactionResponse::from);

    }
}