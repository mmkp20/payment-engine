CREATE TABLE outbox_events (
                               event_id UUID PRIMARY KEY,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                               attempts INTEGER NOT NULL DEFAULT 0,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               published_at TIMESTAMPTZ,

                               CONSTRAINT chk_outbox_status
                                   CHECK (status IN ('PENDING', 'PUBLISHED')),

                               CONSTRAINT chk_outbox_attempts
                                   CHECK (attempts >= 0),

                               CONSTRAINT fk_outbox_payment
                                   FOREIGN KEY (aggregate_id)
                                       REFERENCES payments(id)
                                       ON DELETE CASCADE
);

CREATE INDEX idx_outbox_pending_events
    ON outbox_events(created_at)
    WHERE status = 'PENDING';