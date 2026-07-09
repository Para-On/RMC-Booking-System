CREATE TABLE refund_policy (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                    VARCHAR(100) NOT NULL DEFAULT 'Default',
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    full_cutoff_value       INT NOT NULL DEFAULT 48,
    full_cutoff_unit        VARCHAR(10) NOT NULL DEFAULT 'HOURS',
    partial_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    partial_refund_percent  INT NOT NULL DEFAULT 50,
    check_in_time           TIME NOT NULL DEFAULT '14:00:00',
    timezone                VARCHAR(50) NOT NULL DEFAULT 'Asia/Manila',
    description             TEXT NULL,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO refund_policy (
    name, enabled, full_cutoff_value, full_cutoff_unit,
    partial_enabled, partial_refund_percent, check_in_time, timezone,
    description, active
) VALUES (
    'Default hotel policy',
    TRUE,
    48,
    'HOURS',
    TRUE,
    50,
    '14:00:00',
    'Asia/Manila',
    'Free cancellation until 48 hours before check-in. After that, a 50% refund applies until check-in time.',
    TRUE
);

CREATE TABLE booking_refund_policy_snapshot (
    booking_id              BIGINT PRIMARY KEY,
    refund_policy_id        BIGINT NULL,
    enabled                 BOOLEAN NOT NULL,
    full_cutoff_value       INT NOT NULL,
    full_cutoff_unit        VARCHAR(10) NOT NULL,
    partial_enabled         BOOLEAN NOT NULL,
    partial_refund_percent  INT NOT NULL,
    check_in_time           TIME NOT NULL,
    timezone                VARCHAR(50) NOT NULL,
    description             TEXT NULL,
    room_refundable         BOOLEAN NOT NULL DEFAULT TRUE,
    snapshotted_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_policy_snapshot_booking
        FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT fk_booking_policy_snapshot_policy
        FOREIGN KEY (refund_policy_id) REFERENCES refund_policy (id)
);

ALTER TABLE booking
    ADD COLUMN cancellation_reason VARCHAR(500) NULL AFTER refund_status,
    ADD COLUMN refund_percent_applied INT NULL AFTER cancellation_reason,
    ADD COLUMN deduction_amount DECIMAL(12, 2) NULL AFTER refund_percent_applied,
    ADD COLUMN amount_paid_at_cancel DECIMAL(12, 2) NULL AFTER deduction_amount;

INSERT INTO booking_refund_policy_snapshot (
    booking_id, refund_policy_id, enabled, full_cutoff_value, full_cutoff_unit,
    partial_enabled, partial_refund_percent, check_in_time, timezone,
    description, room_refundable, snapshotted_at
)
SELECT
    b.id,
    (SELECT id FROM refund_policy WHERE active = TRUE ORDER BY id LIMIT 1),
    TRUE,
    COALESCE(rp.refund_window_hours, 48),
    'HOURS',
    COALESCE(rp.allow_late_cancellation, TRUE),
    COALESCE(rp.late_cancel_refund_percent, 50),
    '14:00:00',
    'Asia/Manila',
    rp.cancellation_policy,
    COALESCE(rt.refundable, TRUE),
    COALESCE(b.created_at, CURRENT_TIMESTAMP)
FROM booking b
JOIN rate_plan rp ON rp.id = b.rate_plan_id
JOIN room_type rt ON rt.id = b.room_type_id
WHERE b.status IN ('CONFIRMED', 'CONFIRMED_PAY_LATER', 'CANCELLED', 'CHECKED_IN', 'CHECKED_OUT', 'NO_SHOW')
  AND NOT EXISTS (
      SELECT 1 FROM booking_refund_policy_snapshot s WHERE s.booking_id = b.id
  );

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'settings-refund-policy',
    'Refund policy',
    '/staff/settings/refund-policy',
    'settings',
    allowed_roles,
    7,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'settings'
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'settings-refund-policy');
