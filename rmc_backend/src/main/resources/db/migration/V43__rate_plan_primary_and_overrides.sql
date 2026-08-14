-- Primary nightly rate on rate_plan; daily_rate overrides for specific nights.
ALTER TABLE rate_plan
    ADD COLUMN base_nightly_rate DECIMAL(12, 2) NULL AFTER pay_later_cutoff_hours;

ALTER TABLE daily_rate
    ADD COLUMN is_override TINYINT(1) NOT NULL DEFAULT 0 AFTER currency;

UPDATE rate_plan rp
INNER JOIN daily_rate today
    ON today.rate_plan_id = rp.id
   AND today.rate_date = CURDATE()
SET rp.base_nightly_rate = today.amount
WHERE rp.base_nightly_rate IS NULL;

UPDATE rate_plan rp
INNER JOIN daily_rate dr ON dr.rate_plan_id = rp.id
INNER JOIN (
    SELECT rate_plan_id, MAX(rate_date) AS max_date
    FROM daily_rate
    GROUP BY rate_plan_id
) latest
    ON latest.rate_plan_id = dr.rate_plan_id
   AND latest.max_date = dr.rate_date
SET rp.base_nightly_rate = dr.amount
WHERE rp.base_nightly_rate IS NULL;
