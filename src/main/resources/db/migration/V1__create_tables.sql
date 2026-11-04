CREATE TABLE accounts (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120)   NOT NULL,
    balance    NUMERIC(19, 2) NOT NULL CHECK (balance >= 0),
    created_at TIMESTAMP      NOT NULL
);

CREATE TABLE transfers (
    id              BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(64)    NOT NULL UNIQUE,
    from_account_id BIGINT         NOT NULL REFERENCES accounts (id),
    to_account_id   BIGINT         NOT NULL REFERENCES accounts (id),
    amount          NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    created_at      TIMESTAMP      NOT NULL,
    CHECK (from_account_id <> to_account_id)
);

CREATE INDEX idx_transfers_from_account ON transfers (from_account_id, created_at);
CREATE INDEX idx_transfers_to_account ON transfers (to_account_id, created_at);
