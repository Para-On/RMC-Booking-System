CREATE TABLE room_service_addon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    subtitle VARCHAR(200),
    details TEXT,
    image_url VARCHAR(512),
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    price DECIMAL(12, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE room_item_addon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    price DECIMAL(12, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE booking_service_selection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    service_addon_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL DEFAULT 0,
    line_total DECIMAL(12, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_bss_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT fk_bss_service FOREIGN KEY (service_addon_id) REFERENCES room_service_addon (id)
);

CREATE TABLE booking_item_selection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    item_addon_id BIGINT NOT NULL,
    item_name VARCHAR(120) NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    guest_note VARCHAR(500),
    unit_price DECIMAL(12, 2),
    CONSTRAINT fk_bis_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT fk_bis_item FOREIGN KEY (item_addon_id) REFERENCES room_item_addon (id)
);

ALTER TABLE branding_config
    ADD COLUMN footer_text VARCHAR(500),
    ADD COLUMN footer_contact_email VARCHAR(120),
    ADD COLUMN footer_contact_phone VARCHAR(40),
    ADD COLUMN footer_copyright VARCHAR(200);

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'rooms-extras',
    'Extras',
    '/staff/rooms/extras',
    'bed',
    allowed_roles,
    15,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'rooms';
