INSERT INTO room_config_option (option_type, label, sort_order) VALUES
    ('ROOM_STATUS', 'Available', 1),
    ('ROOM_STATUS', 'Out of order', 2),
    ('ROOM_STATUS', 'Under maintenance', 3),
    ('ROOM_STATUS', 'Deep cleaning', 4);

ALTER TABLE room_unit
    ADD COLUMN status_option_id BIGINT NULL,
    ADD CONSTRAINT fk_room_unit_status_option FOREIGN KEY (status_option_id) REFERENCES room_config_option (id);

UPDATE room_unit ru
    JOIN room_config_option o ON o.option_type = 'ROOM_STATUS' AND o.label = 'Available'
SET ru.status_option_id = o.id
WHERE ru.status IN ('AVAILABLE', 'OCCUPIED');

UPDATE room_unit ru
    JOIN room_config_option o ON o.option_type = 'ROOM_STATUS' AND o.label = 'Out of order'
SET ru.status_option_id = o.id
WHERE ru.status = 'OUT_OF_ORDER';

UPDATE room_unit SET status = 'AVAILABLE' WHERE status = 'OCCUPIED';
