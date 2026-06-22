CREATE TABLE system_config (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key  VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(255) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE room_type (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                     VARCHAR(100) NOT NULL,
    description              TEXT,
    max_adults               INT NOT NULL,
    max_children             INT NOT NULL DEFAULT 0,
    total_capacity           INT NOT NULL,
    overbooking_buffer       INT NOT NULL DEFAULT 0,
    min_advance_booking_hours INT NOT NULL DEFAULT 0,
    max_advance_booking_days  INT NOT NULL DEFAULT 365,
    active                   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE room_unit (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type_id BIGINT NOT NULL,
    room_number  VARCHAR(20) NOT NULL,
    floor_label  VARCHAR(20),
    status       VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT fk_room_unit_type FOREIGN KEY (room_type_id) REFERENCES room_type (id),
    CONSTRAINT uq_room_number UNIQUE (room_number)
);

CREATE TABLE rate_plan (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type_id          BIGINT NOT NULL,
    name                  VARCHAR(100) NOT NULL,
    cancellation_policy   VARCHAR(255),
    refund_window_hours   INT NOT NULL DEFAULT 24,
    hold_ttl_minutes      INT NOT NULL DEFAULT 15,
    pay_later_cutoff_hours INT,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_rate_plan_room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id)
);

CREATE TABLE daily_rate (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    rate_plan_id BIGINT NOT NULL,
    rate_date    DATE NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    currency     VARCHAR(3) NOT NULL DEFAULT 'PHP',
    CONSTRAINT fk_daily_rate_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id),
    CONSTRAINT uq_daily_rate UNIQUE (rate_plan_id, rate_date)
);

CREATE TABLE guest (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    email               VARCHAR(255) NOT NULL,
    full_name           VARCHAR(255) NOT NULL,
    phone               VARCHAR(50) NOT NULL,
    consent_timestamp   TIMESTAMP NOT NULL,
    dpa_consent_version VARCHAR(20) NOT NULL,
    age_confirmed_at    TIMESTAMP NOT NULL
);

CREATE TABLE booking (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type_id   BIGINT NOT NULL,
    rate_plan_id   BIGINT NOT NULL,
    guest_id       BIGINT NOT NULL,
    reference      VARCHAR(30) NOT NULL UNIQUE,
    check_in_date  DATE NOT NULL,
    check_out_date DATE NOT NULL,
    status         VARCHAR(30) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    quoted_total   DECIMAL(12, 2) NOT NULL,
    currency       VARCHAR(3) NOT NULL DEFAULT 'PHP',
    checked_in_at  TIMESTAMP NULL,
    checked_out_at TIMESTAMP NULL,
    room_unit_id   BIGINT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at     TIMESTAMP NULL,
    CONSTRAINT fk_booking_room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id),
    CONSTRAINT fk_booking_rate_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id),
    CONSTRAINT fk_booking_guest FOREIGN KEY (guest_id) REFERENCES guest (id),
    CONSTRAINT fk_booking_room_unit FOREIGN KEY (room_unit_id) REFERENCES room_unit (id)
);

CREATE TABLE inventory_hold (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id   BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    hold_date    DATE NOT NULL,
    held_count   INT NOT NULL DEFAULT 1,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at   TIMESTAMP NULL,
    CONSTRAINT fk_hold_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT fk_hold_room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id)
);

CREATE INDEX idx_hold_room_date ON inventory_hold (room_type_id, hold_date, status);

CREATE TABLE booking_ledger (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT NOT NULL,
    entry_type      VARCHAR(20) NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    maya_reference  VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_booking FOREIGN KEY (booking_id) REFERENCES booking (id)
);

CREATE TABLE booking_audit_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id     BIGINT NOT NULL,
    from_status    VARCHAR(30),
    to_status      VARCHAR(30) NOT NULL,
    trigger_source VARCHAR(50) NOT NULL,
    staff_user_id  BIGINT NULL,
    reason         VARCHAR(500),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_booking FOREIGN KEY (booking_id) REFERENCES booking (id)
);
