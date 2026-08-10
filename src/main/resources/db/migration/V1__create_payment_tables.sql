CREATE TABLE payments (
                          id UUID PRIMARY KEY,
                          amount NUMERIC(19, 2) NOT NULL,
                          currency CHAR(3) NOT NULL,
                          status VARCHAR(32) NOT NULL,
                          failure_reason VARCHAR(500),
                          version BIGINT NOT NULL DEFAULT 0,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT chk_payments_amount_positive
                              CHECK (amount > 0),

                          CONSTRAINT chk_payments_currency
                              CHECK (currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_payments_status
        CHECK (
            status IN (
                'CREATED',
                'PROCESSING',
                'SUCCEEDED',
                'FAILED'
            )
        )
);

CREATE TABLE idempotency_records (
                                     idempotency_key VARCHAR(255) PRIMARY KEY,
                                     payment_id UUID NOT NULL UNIQUE,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT chk_idempotency_key_not_blank
                                         CHECK (btrim(idempotency_key) <> ''),

                                     CONSTRAINT fk_idempotency_payment
                                         FOREIGN KEY (payment_id)
                                             REFERENCES payments(id)
                                             ON DELETE CASCADE
);

CREATE INDEX idx_payments_status
    ON payments(status);

CREATE INDEX idx_payments_created_at
    ON payments(created_at);