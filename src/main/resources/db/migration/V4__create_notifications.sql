CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT       NOT NULL,
    transfer_id BIGINT       NOT NULL,
    direction   VARCHAR(10)  NOT NULL,
    message     VARCHAR(255) NOT NULL,
    status      VARCHAR(10)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_notifications_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_notifications_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id),
    CONSTRAINT ck_notifications_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_notifications_status CHECK (status IN ('SENT', 'FAILED'))
);

CREATE INDEX idx_notifications_account ON notifications (account_id, created_at);
