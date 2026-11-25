-- 기존 회원·계좌는 보존하고 거래 Entity에 대응하는 테이블만 추가한다.
CREATE TABLE financial_transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    type VARCHAR(30) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    memo VARCHAR(500),
    transfer_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_financial_transactions_type
        CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER_IN', 'TRANSFER_OUT'))
);

CREATE INDEX idx_financial_transactions_account_occurred_id
    ON financial_transactions(account_id, occurred_at, id);
