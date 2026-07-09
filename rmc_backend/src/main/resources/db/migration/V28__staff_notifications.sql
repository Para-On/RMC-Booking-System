CREATE TABLE staff_notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(50)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    message     VARCHAR(500) NOT NULL,
    booking_id  BIGINT       NULL,
    link_path   VARCHAR(255) NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_notification_booking FOREIGN KEY (booking_id) REFERENCES booking (id)
);

CREATE INDEX idx_staff_notification_created ON staff_notification (created_at DESC);

CREATE TABLE staff_notification_read (
    staff_user_id     BIGINT    NOT NULL,
    notification_id   BIGINT    NOT NULL,
    read_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (staff_user_id, notification_id),
    CONSTRAINT fk_snr_staff FOREIGN KEY (staff_user_id) REFERENCES staff_user (id),
    CONSTRAINT fk_snr_notification FOREIGN KEY (notification_id) REFERENCES staff_notification (id) ON DELETE CASCADE
);
