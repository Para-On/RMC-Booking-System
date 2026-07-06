CREATE TABLE room_config_option (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    option_type  VARCHAR(32)  NOT NULL,
    label        VARCHAR(100) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_room_config_option_type_label UNIQUE (option_type, label)
);

CREATE INDEX idx_room_config_option_type ON room_config_option (option_type, sort_order, label);

ALTER TABLE room_type
    ADD COLUMN room_category_id BIGINT NULL,
    ADD COLUMN room_view_id      BIGINT NULL,
    ADD COLUMN bed_type_id       BIGINT NULL,
    ADD CONSTRAINT fk_room_type_category FOREIGN KEY (room_category_id) REFERENCES room_config_option (id),
    ADD CONSTRAINT fk_room_type_view FOREIGN KEY (room_view_id) REFERENCES room_config_option (id),
    ADD CONSTRAINT fk_room_type_bed_type FOREIGN KEY (bed_type_id) REFERENCES room_config_option (id);

INSERT INTO room_config_option (option_type, label, sort_order) VALUES
    ('ROOM_CATEGORY', 'Standard', 1),
    ('ROOM_CATEGORY', 'Deluxe', 2),
    ('ROOM_CATEGORY', 'Suite', 3),
    ('ROOM_VIEW', 'City View', 1),
    ('ROOM_VIEW', 'Garden View', 2),
    ('ROOM_VIEW', 'Pool View', 3),
    ('ROOM_VIEW', 'No View', 4),
    ('BED_TYPE', 'Queen', 1),
    ('BED_TYPE', 'King', 2),
    ('BED_TYPE', 'Twin', 3),
    ('BED_TYPE', 'Double', 4);
