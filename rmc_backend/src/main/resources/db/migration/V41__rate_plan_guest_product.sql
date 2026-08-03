-- Rate plans own guest-facing product fields; catalog remains inventory (name + units).

ALTER TABLE rate_plan
    ADD COLUMN description TEXT NULL AFTER name,
    ADD COLUMN max_adults INT NOT NULL DEFAULT 2 AFTER description,
    ADD COLUMN max_children INT NOT NULL DEFAULT 0 AFTER max_adults,
    ADD COLUMN square_meters DECIMAL(8, 2) NULL AFTER max_children,
    ADD COLUMN amenities JSON NULL AFTER square_meters,
    ADD COLUMN room_category_id BIGINT NULL AFTER amenities,
    ADD COLUMN room_view_id BIGINT NULL AFTER room_category_id,
    ADD COLUMN bed_type_id BIGINT NULL AFTER room_view_id;

ALTER TABLE rate_plan
    ADD CONSTRAINT fk_rate_plan_category FOREIGN KEY (room_category_id) REFERENCES room_config_option (id),
    ADD CONSTRAINT fk_rate_plan_view FOREIGN KEY (room_view_id) REFERENCES room_config_option (id),
    ADD CONSTRAINT fk_rate_plan_bed FOREIGN KEY (bed_type_id) REFERENCES room_config_option (id);

CREATE TABLE rate_plan_image (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    rate_plan_id BIGINT NOT NULL,
    image_url    VARCHAR(512) NOT NULL,
    sort_order   INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rate_plan_image_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id) ON DELETE CASCADE
);

CREATE INDEX idx_rate_plan_image_plan ON rate_plan_image (rate_plan_id, sort_order);

-- Backfill product fields from room_type onto each rate plan
UPDATE rate_plan rp
INNER JOIN room_type rt ON rt.id = rp.room_type_id
SET
    rp.description = COALESCE(rp.description, rt.description),
    rp.max_adults = COALESCE(NULLIF(rp.max_adults, 0), rt.max_adults, 2),
    rp.max_children = COALESCE(rt.max_children, 0),
    rp.square_meters = COALESCE(rp.square_meters, rt.square_meters),
    rp.amenities = COALESCE(rp.amenities, rt.amenities),
    rp.room_category_id = COALESCE(rp.room_category_id, rt.room_category_id),
    rp.room_view_id = COALESCE(rp.room_view_id, rt.room_view_id),
    rp.bed_type_id = COALESCE(rp.bed_type_id, rt.bed_type_id);

-- Copy room_type images onto each rate plan under that type (once)
INSERT INTO rate_plan_image (rate_plan_id, image_url, sort_order)
SELECT rp.id, rti.image_url, rti.sort_order
FROM rate_plan rp
INNER JOIN room_type_image rti ON rti.room_type_id = rp.room_type_id
WHERE NOT EXISTS (
    SELECT 1 FROM rate_plan_image existing WHERE existing.rate_plan_id = rp.id
);
