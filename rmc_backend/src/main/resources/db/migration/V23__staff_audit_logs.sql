CREATE TABLE staff_login_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_user_id   BIGINT NULL,
    email_attempt   VARCHAR(255) NULL,
    full_name       VARCHAR(120) NULL,
    event_type      VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    ip_address      VARCHAR(45) NULL,
    failure_reason  VARCHAR(500) NULL,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_login_audit_staff FOREIGN KEY (staff_user_id) REFERENCES staff_user (id)
);

CREATE INDEX idx_staff_login_audit_created ON staff_login_audit_log (created_at DESC);

CREATE TABLE staff_activity_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_user_id   BIGINT NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    role            VARCHAR(30) NOT NULL,
    action_type     VARCHAR(20) NOT NULL,
    module_key      VARCHAR(50) NOT NULL,
    module_label    VARCHAR(100) NOT NULL,
    request_method  VARCHAR(10) NULL,
    request_path    VARCHAR(500) NULL,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_activity_audit_staff FOREIGN KEY (staff_user_id) REFERENCES staff_user (id)
);

CREATE INDEX idx_staff_activity_audit_created ON staff_activity_audit_log (created_at DESC);

UPDATE staff_nav_module
SET path = '#'
WHERE module_key = 'settings'
  AND path = '/staff/settings';

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'settings-general',
    'General',
    '/staff/settings',
    'settings',
    allowed_roles,
    5,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'settings'
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'settings-general');

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'settings-audit',
    'Audit logs',
    '/staff/settings/audit',
    'layout',
    allowed_roles,
    10,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'settings'
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'settings-audit');
