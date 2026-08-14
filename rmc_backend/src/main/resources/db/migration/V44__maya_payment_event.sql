CREATE TABLE maya_payment_event (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    source             VARCHAR(32) NOT NULL,
    booking_reference  VARCHAR(64) NULL,
    checkout_id        VARCHAR(100) NULL,
    checkout_status    VARCHAR(40) NULL,
    payment_status     VARCHAR(40) NULL,
    amount             DECIMAL(12, 2) NULL,
    currency           VARCHAR(3) NULL,
    payload_sha256     CHAR(64) NOT NULL,
    payload_json       TEXT NOT NULL,
    correlation_id     VARCHAR(128) NULL,
    received_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_maya_payment_event_ref ON maya_payment_event (booking_reference, received_at);
CREATE INDEX idx_maya_payment_event_hash ON maya_payment_event (payload_sha256);
CREATE INDEX idx_maya_payment_event_checkout ON maya_payment_event (checkout_id);
