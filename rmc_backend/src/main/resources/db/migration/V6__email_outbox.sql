CREATE TABLE email_outbox (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id     BIGINT NOT NULL,
    recipient      VARCHAR(255) NOT NULL,
    subject        VARCHAR(255) NOT NULL,
    body           TEXT NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts       INT NOT NULL DEFAULT 0,
    max_attempts   INT NOT NULL DEFAULT 5,
    last_error     VARCHAR(500),
    next_retry_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at        TIMESTAMP NULL,
    CONSTRAINT fk_email_outbox_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT uq_email_outbox_booking UNIQUE (booking_id)
);

CREATE INDEX idx_email_outbox_pending ON email_outbox (status, next_retry_at);
