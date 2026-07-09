INSERT INTO system_config (config_key, config_value, description) VALUES
    ('manualRefundEnabled', 'false', 'Allow staff to record manual refunds (GCash, bank transfer, cash) when Maya cannot process a refund')
ON DUPLICATE KEY UPDATE description = VALUES(description);

SET @has_email_kind := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'email_outbox'
      AND COLUMN_NAME = 'email_kind'
);
SET @ddl := IF(
    @has_email_kind = 0,
    'ALTER TABLE email_outbox ADD COLUMN email_kind VARCHAR(30) NOT NULL DEFAULT ''CONFIRMATION'' AFTER booking_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_new_index := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'email_outbox'
      AND INDEX_NAME = 'uq_email_outbox_booking_kind'
);
SET @ddl := IF(
    @has_new_index = 0,
    'ALTER TABLE email_outbox ADD CONSTRAINT uq_email_outbox_booking_kind UNIQUE (booking_id, email_kind)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_old_index := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'email_outbox'
      AND INDEX_NAME = 'uq_email_outbox_booking'
);
SET @ddl := IF(
    @has_old_index > 0,
    'ALTER TABLE email_outbox DROP INDEX uq_email_outbox_booking',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
