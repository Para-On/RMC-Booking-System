INSERT INTO system_config (config_key, config_value, description) VALUES
('serviceChargePercent', '10', 'Service charge percentage applied before VAT'),
('vatPercent', '12', 'VAT percentage applied after service charge'),
('dpaConsentVersion', '1.0', 'Current DPA consent version shown at checkout');

INSERT INTO room_type (name, description, max_adults, max_children, total_capacity, overbooking_buffer, min_advance_booking_hours, max_advance_booking_days) VALUES
('Standard Room', 'Comfortable room with queen bed, ideal for couples or solo travelers.', 2, 1, 5, 0, 2, 90),
('Deluxe Room', 'Spacious room with king bed and city view.', 2, 2, 3, 0, 2, 90);

INSERT INTO room_unit (room_type_id, room_number, floor_label, status) VALUES
(1, '101', '1', 'AVAILABLE'),
(1, '102', '1', 'AVAILABLE'),
(1, '103', '1', 'AVAILABLE'),
(1, '104', '2', 'AVAILABLE'),
(1, '105', '2', 'AVAILABLE'),
(2, '201', '2', 'AVAILABLE'),
(2, '202', '2', 'AVAILABLE'),
(2, '203', '3', 'AVAILABLE');

INSERT INTO rate_plan (room_type_id, name, cancellation_policy, refund_window_hours, hold_ttl_minutes, pay_later_cutoff_hours) VALUES
(1, 'Standard Flexible', 'Free cancellation up to 24 hours before check-in.', 24, 15, 24),
(2, 'Deluxe Flexible', 'Free cancellation up to 48 hours before check-in.', 48, 15, 24);

-- Seed daily rates for 120 days from today (uses CURDATE())
INSERT INTO daily_rate (rate_plan_id, rate_date, amount, currency)
SELECT 1, DATE_ADD(CURDATE(), INTERVAL seq DAY), 2500.00, 'PHP'
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 AS seq
    FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
    CROSS JOIN (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
    CROSS JOIN (SELECT 0 AS N UNION SELECT 1) c
) days
WHERE seq < 120;

INSERT INTO daily_rate (rate_plan_id, rate_date, amount, currency)
SELECT 2, DATE_ADD(CURDATE(), INTERVAL seq DAY), 3800.00, 'PHP'
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 AS seq
    FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
    CROSS JOIN (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
    CROSS JOIN (SELECT 0 AS N UNION SELECT 1) c
) days
WHERE seq < 120;
