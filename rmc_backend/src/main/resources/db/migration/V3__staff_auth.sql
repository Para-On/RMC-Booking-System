CREATE TABLE staff_user (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    full_name             VARCHAR(255) NOT NULL,
    role                  VARCHAR(30) NOT NULL,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until          TIMESTAMP NULL,
    last_login_at         TIMESTAMP NULL,
    mfa_enabled           BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret            VARCHAR(255) NULL,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_token (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_user_id  BIGINT NOT NULL,
    token_hash     VARCHAR(255) NOT NULL UNIQUE,
    expires_at     TIMESTAMP NOT NULL,
    revoked_at     TIMESTAMP NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_staff FOREIGN KEY (staff_user_id) REFERENCES staff_user (id)
);

CREATE INDEX idx_refresh_token_staff ON refresh_token (staff_user_id);

ALTER TABLE booking_audit_log
    ADD CONSTRAINT fk_audit_staff FOREIGN KEY (staff_user_id) REFERENCES staff_user (id);

INSERT INTO system_config (config_key, config_value, description) VALUES
('jwtLockoutThreshold', '5', 'Failed login attempts before account lockout'),
('jwtLockoutMinutes', '15', 'Account lockout duration in minutes');

-- Dev seed accounts: frontdesk@rmc.local / staff123, manager@rmc.local / manager123
INSERT INTO staff_user (email, password_hash, full_name, role) VALUES
('frontdesk@rmc.local', '$2a$10$PIrIVFkFNf7KAsRTuWth0u8FlIhu1s8km555eOXbNE6TgNRPTz0My', 'Front Desk User', 'FRONT_DESK'),
('manager@rmc.local', '$2a$10$FEWSKkUdLu7ELyBECRBHTONifG7.wPcG9Zm78ye2txxC7sIm3cgue', 'Hotel Manager', 'MANAGER');
