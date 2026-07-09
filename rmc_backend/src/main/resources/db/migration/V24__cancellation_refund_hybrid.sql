ALTER TABLE rate_plan
    ADD COLUMN late_cancel_refund_percent INT NOT NULL DEFAULT 50 AFTER refund_window_hours,
    ADD COLUMN allow_late_cancellation BOOLEAN NOT NULL DEFAULT TRUE AFTER late_cancel_refund_percent;

ALTER TABLE booking
    ADD COLUMN cancelled_at TIMESTAMP NULL AFTER maya_checkout_id,
    ADD COLUMN cancellation_tier VARCHAR(20) NULL AFTER cancelled_at,
    ADD COLUMN refund_eligible_amount DECIMAL(12, 2) NULL AFTER cancellation_tier,
    ADD COLUMN refund_status VARCHAR(20) NULL AFTER refund_eligible_amount;
