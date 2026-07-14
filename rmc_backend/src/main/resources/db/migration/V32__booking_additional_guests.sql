-- Deduplicate guests by email (keep lowest id), then enforce uniqueness.
-- Additional occupants are stored per booking and optionally linked to a guest profile.

UPDATE booking b
INNER JOIN guest g ON b.guest_id = g.id
INNER JOIN (
    SELECT LOWER(email) AS em, MIN(id) AS keep_id
    FROM guest
    GROUP BY LOWER(email)
    HAVING COUNT(*) > 1
) d ON LOWER(g.email) = d.em AND g.id <> d.keep_id
SET b.guest_id = d.keep_id;

UPDATE guest keep_g
INNER JOIN (
    SELECT LOWER(email) AS em, MIN(id) AS keep_id, MAX(id) AS latest_id
    FROM guest
    GROUP BY LOWER(email)
    HAVING COUNT(*) > 1
) d ON keep_g.id = d.keep_id
INNER JOIN guest latest ON latest.id = d.latest_id
SET keep_g.phone = latest.phone,
    keep_g.full_name = latest.full_name,
    keep_g.consent_timestamp = latest.consent_timestamp,
    keep_g.dpa_consent_version = latest.dpa_consent_version,
    keep_g.age_confirmed_at = latest.age_confirmed_at;

DELETE g
FROM guest g
INNER JOIN (
    SELECT LOWER(email) AS em, MIN(id) AS keep_id
    FROM guest
    GROUP BY LOWER(email)
    HAVING COUNT(*) > 1
) d ON LOWER(g.email) = d.em AND g.id <> d.keep_id;

CREATE UNIQUE INDEX uk_guest_email_lower ON guest ((LOWER(email)));

CREATE TABLE booking_additional_guest (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT NOT NULL,
    guest_id    BIGINT NULL,
    full_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NULL,
    phone       VARCHAR(50) NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_bag_booking FOREIGN KEY (booking_id) REFERENCES booking (id) ON DELETE CASCADE,
    CONSTRAINT fk_bag_guest FOREIGN KEY (guest_id) REFERENCES guest (id)
);

CREATE INDEX idx_bag_booking ON booking_additional_guest (booking_id);
CREATE INDEX idx_bag_guest ON booking_additional_guest (guest_id);
