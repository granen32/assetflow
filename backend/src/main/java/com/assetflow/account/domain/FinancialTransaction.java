package com.assetflow.account.domain;

import com.assetflow.transaction.domain.TransactionType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "financial_transactions")
public class FinancialTransaction {
    @Id @GeneratedValue (strategy = jakarta.persistence.GenerationType.IDENTITY)

}
