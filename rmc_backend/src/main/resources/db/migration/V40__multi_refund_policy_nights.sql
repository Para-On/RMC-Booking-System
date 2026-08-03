-- Multi named refund policies, nights deduction, rate_plan.refund_policy_id;
-- disable dedicated rooms-rate-plans nav (rate plans live on Create room tabs).

ALTER TABLE refund_policy
    ADD COLUMN nights_deduction_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER partial_refund_percent,
    ADD COLUMN nights_deducted INT NOT NULL DEFAULT 1 AFTER nights_deduction_enabled,
    ADD COLUMN refundable TINYINT(1) NOT NULL DEFAULT 1 AFTER description;

ALTER TABLE booking_refund_policy_snapshot
    ADD COLUMN nights_deduction_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER partial_refund_percent,
    ADD COLUMN nights_deducted INT NOT NULL DEFAULT 1 AFTER nights_deduction_enabled;

ALTER TABLE rate_plan
    ADD COLUMN refund_policy_id BIGINT NULL AFTER room_type_id;

-- Link each rate plan to a dedicated policy cloned from its denormalized fields
-- (keeps per-plan policy semantics after moving to FK references).
INSERT INTO refund_policy (
    name, enabled, full_cutoff_value, full_cutoff_unit, partial_enabled, partial_refund_percent,
    nights_deduction_enabled, nights_deducted, check_in_time, timezone, description, refundable, active,
    created_at, updated_at
)
SELECT
    CONCAT('Plan ', rp.id, ' — ', LEFT(rp.name, 60)),
    COALESCE(rp.policy_enabled, 1),
    COALESCE(rp.full_cutoff_value, 48),
    COALESCE(rp.full_cutoff_unit, 'HOURS'),
    COALESCE(rp.partial_enabled, 1),
    COALESCE(rp.partial_refund_percent, 50),
    0,
    1,
    COALESCE(rp.check_in_time, '14:00:00'),
    COALESCE(rp.timezone, 'Asia/Manila'),
    rp.policy_description,
    COALESCE(rp.refundable, 0),
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM rate_plan rp
WHERE rp.refund_policy_id IS NULL;

UPDATE rate_plan rp
INNER JOIN refund_policy pol ON pol.name = CONCAT('Plan ', rp.id, ' — ', LEFT(rp.name, 60))
SET rp.refund_policy_id = pol.id
WHERE rp.refund_policy_id IS NULL;

-- Fallback: any remaining plans point at first active policy
UPDATE rate_plan
SET refund_policy_id = (SELECT id FROM refund_policy WHERE active = 1 ORDER BY id LIMIT 1)
WHERE refund_policy_id IS NULL
  AND EXISTS (SELECT 1 FROM refund_policy WHERE active = 1);

ALTER TABLE rate_plan
    MODIFY COLUMN refund_policy_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_rate_plan_refund_policy
        FOREIGN KEY (refund_policy_id) REFERENCES refund_policy (id);

CREATE INDEX idx_rate_plan_refund_policy ON rate_plan (refund_policy_id);

UPDATE booking_refund_policy_snapshot s
INNER JOIN booking b ON b.id = s.booking_id
INNER JOIN rate_plan rp ON rp.id = b.rate_plan_id
INNER JOIN refund_policy pol ON pol.id = rp.refund_policy_id
SET
    s.refund_policy_id = pol.id,
    s.nights_deduction_enabled = pol.nights_deduction_enabled,
    s.nights_deducted = pol.nights_deducted
WHERE s.nights_deduction_enabled = 0;

UPDATE staff_nav_module
SET enabled = 0
WHERE module_key = 'rooms-rate-plans';
