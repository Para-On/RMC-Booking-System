CREATE TABLE booking_additional_charge (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id           BIGINT NOT NULL,
    description          VARCHAR(255) NOT NULL,
    amount               DECIMAL(12, 2) NOT NULL,
    currency             VARCHAR(3) NOT NULL DEFAULT 'PHP',
    status               VARCHAR(30) NOT NULL,
    payment_method       VARCHAR(30) NULL,
    maya_checkout_id     VARCHAR(100) NULL,
    maya_request_ref     VARCHAR(100) NULL,
    created_by_staff_id  BIGINT NULL,
    approved_by_staff_id BIGINT NULL,
    paid_at              TIMESTAMP NULL,
    approved_at          TIMESTAMP NULL,
    rejected_at          TIMESTAMP NULL,
    rejection_reason     VARCHAR(500) NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_addl_charge_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT uq_addl_charge_maya_request_ref UNIQUE (maya_request_ref),
    CONSTRAINT chk_addl_charge_amount CHECK (amount > 0)
);

CREATE INDEX idx_addl_charge_booking ON booking_additional_charge (booking_id, status);
CREATE INDEX idx_addl_charge_maya_checkout ON booking_additional_charge (maya_checkout_id);
