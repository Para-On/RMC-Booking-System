ALTER TABLE booking ADD COLUMN maya_checkout_id VARCHAR(100) NULL;
CREATE INDEX idx_booking_maya_checkout ON booking (maya_checkout_id);
