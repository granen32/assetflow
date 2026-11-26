package com.assetflow.transaction.dto;

import com.assetflow.transaction.domain.FinancialTransaction;
import com.assetflow.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        Long id,
        Long accountId,
        TransactionType type,
        BigDecimal amount,
        Instant occurredAt,
        String memo,
        UUID transferId,
        Instant createdAt) {

    public static TransactionResponse from(FinancialTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getOccurredAt(),
                transaction.getMemo(),
                transaction.getTransferId(),
                transaction.getCreatedAt());
    }

}
