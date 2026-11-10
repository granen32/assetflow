package com.assetflow.transaction.domain;

/**
 * ㅏ
 * 허용되는 거래 유형을 문자열 대신 enum으로 제한한다.
 */
public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER_IN,
    TRANSFER_OUT,
}