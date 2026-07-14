-- Promos / discounts + Settings nav submodule
CREATE TABLE promo (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(500) NULL,
    discount_type   VARCHAR(20) NOT NULL,
    discount_value  DECIMAL(12, 2) NOT NULL,
    starts_on       DATE NOT NULL,
    ends_on         DATE NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NULL,
    CONSTRAINT chk_promo_dates CHECK (ends_on >= starts_on),
    CONSTRAINT chk_promo_value CHECK (discount_value > 0),
    CONSTRAINT chk_promo_type CHECK (discount_type IN ('PERCENT', 'FIXED'))
);

CREATE TABLE promo_room_type (
    promo_id     BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    PRIMARY KEY (promo_id, room_type_id),
    CONSTRAINT fk_prt_promo FOREIGN KEY (promo_id) REFERENCES promo (id) ON DELETE CASCADE,
    CONSTRAINT fk_prt_room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id) ON DELETE CASCADE
);

ALTER TABLE booking
    ADD COLUMN promo_id BIGINT NULL,
    ADD COLUMN promo_name VARCHAR(120) NULL,
    ADD COLUMN promo_discount_amount DECIMAL(12, 2) NULL,
    ADD COLUMN room_total_before_promo DECIMAL(12, 2) NULL,
    ADD CONSTRAINT fk_booking_promo FOREIGN KEY (promo_id) REFERENCES promo (id);

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'settings-promos',
    'Promos & discounts',
    '/staff/settings/promos',
    'settings',
    allowed_roles,
    8,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'settings'
  AND parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'settings-promos');
