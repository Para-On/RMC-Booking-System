-- Guest-entered promo codes (access rates) — distinct from automatic public promos
CREATE TABLE promo_code (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(120) NOT NULL,
    description         VARCHAR(500) NULL,
    promo_type          VARCHAR(20) NOT NULL,
    offer_code          VARCHAR(64) NOT NULL,
    organization_code   VARCHAR(64) NULL,
    discount_type       VARCHAR(20) NOT NULL,
    discount_value      DECIMAL(12, 2) NOT NULL,
    starts_on           DATE NOT NULL,
    ends_on             DATE NOT NULL,
    max_uses            INT NOT NULL,
    used_count          INT NOT NULL DEFAULT 0,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NULL,
    CONSTRAINT chk_promo_code_dates CHECK (ends_on >= starts_on),
    CONSTRAINT chk_promo_code_value CHECK (discount_value > 0),
    CONSTRAINT chk_promo_code_max_uses CHECK (max_uses > 0),
    CONSTRAINT chk_promo_code_used CHECK (used_count >= 0),
    CONSTRAINT chk_promo_code_discount_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT chk_promo_code_type CHECK (promo_type IN ('SPECIAL_RATE', 'CORPORATE', 'AGENCY'))
);

CREATE UNIQUE INDEX uk_promo_code_offer ON promo_code (offer_code);
CREATE INDEX idx_promo_code_org_offer ON promo_code (organization_code, offer_code);

CREATE TABLE promo_code_rate_plan (
    promo_code_id BIGINT NOT NULL,
    rate_plan_id  BIGINT NOT NULL,
    PRIMARY KEY (promo_code_id, rate_plan_id),
    CONSTRAINT fk_pcrp_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_code (id) ON DELETE CASCADE,
    CONSTRAINT fk_pcrp_rate_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id) ON DELETE CASCADE
);

ALTER TABLE booking
    ADD COLUMN promo_code_id BIGINT NULL,
    ADD CONSTRAINT fk_booking_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_code (id);

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'settings-promo-codes',
    'Promo codes',
    '/staff/settings/promo-codes',
    'settings',
    allowed_roles,
    9,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'settings'
  AND parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'settings-promo-codes');
