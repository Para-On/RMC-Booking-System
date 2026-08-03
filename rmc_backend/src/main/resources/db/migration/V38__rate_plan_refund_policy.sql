-- Option A: cancel/refund policy lives on rate_plan; refund_policy remains template for new plans.
ALTER TABLE rate_plan
    ADD COLUMN policy_enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER cancellation_policy,
    ADD COLUMN full_cutoff_value INT NOT NULL DEFAULT 48 AFTER policy_enabled,
    ADD COLUMN full_cutoff_unit VARCHAR(10) NOT NULL DEFAULT 'HOURS' AFTER full_cutoff_value,
    ADD COLUMN partial_enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER full_cutoff_unit,
    ADD COLUMN partial_refund_percent INT NOT NULL DEFAULT 50 AFTER partial_enabled,
    ADD COLUMN check_in_time TIME NOT NULL DEFAULT '14:00:00' AFTER partial_refund_percent,
    ADD COLUMN timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Manila' AFTER check_in_time,
    ADD COLUMN policy_description TEXT NULL AFTER timezone,
    ADD COLUMN refundable TINYINT(1) NOT NULL DEFAULT 0 AFTER policy_description;

-- Map legacy rate_plan cancel fields first.
UPDATE rate_plan
SET
    full_cutoff_value = COALESCE(refund_window_hours, 48),
    full_cutoff_unit = 'HOURS',
    partial_enabled = COALESCE(allow_late_cancellation, 1),
    partial_refund_percent = COALESCE(late_cancel_refund_percent, 50),
    policy_description = cancellation_policy,
    policy_enabled = 1,
    check_in_time = '14:00:00',
    timezone = 'Asia/Manila';

-- Overlay active refund_policy template when one exists.
UPDATE rate_plan rp
INNER JOIN refund_policy pol ON pol.active = 1 AND pol.id = (
    SELECT MIN(id) FROM refund_policy WHERE active = 1
)
SET
    rp.policy_enabled = pol.enabled,
    rp.full_cutoff_value = pol.full_cutoff_value,
    rp.full_cutoff_unit = pol.full_cutoff_unit,
    rp.partial_enabled = pol.partial_enabled,
    rp.partial_refund_percent = pol.partial_refund_percent,
    rp.check_in_time = pol.check_in_time,
    rp.timezone = pol.timezone,
    rp.policy_description = COALESCE(pol.description, rp.policy_description);

-- Refundable from parent room type (marketing flag migration).
UPDATE rate_plan rp
INNER JOIN room_type rt ON rt.id = rp.room_type_id
SET rp.refundable = COALESCE(rt.refundable, 0);
