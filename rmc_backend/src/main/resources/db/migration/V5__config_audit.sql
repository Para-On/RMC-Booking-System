CREATE TABLE configuration_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NULL,
    config_key VARCHAR(100) NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    staff_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_config_audit_staff FOREIGN KEY (staff_user_id) REFERENCES staff_user (id)
);

CREATE INDEX idx_config_audit_created ON configuration_audit_log (created_at DESC);
