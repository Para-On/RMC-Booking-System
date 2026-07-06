-- Allow room numbers to exist before a room type is assigned
ALTER TABLE room_unit MODIFY room_type_id BIGINT NULL;

ALTER TABLE room_type
    ADD COLUMN square_meters DECIMAL(8, 2) NULL,
    ADD COLUMN amenities JSON NULL,
    ADD COLUMN refundable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN free_cancellation BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE room_type_image (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type_id BIGINT NOT NULL,
    image_url    VARCHAR(512) NOT NULL,
    sort_order   INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_room_type_image_type FOREIGN KEY (room_type_id) REFERENCES room_type (id) ON DELETE CASCADE
);

CREATE INDEX idx_room_type_image_type ON room_type_image (room_type_id, sort_order);
