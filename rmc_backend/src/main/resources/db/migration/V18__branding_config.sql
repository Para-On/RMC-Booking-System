CREATE TABLE branding_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(120) NOT NULL DEFAULT 'RMC Booking',
    staff_portal_title VARCHAR(120) NOT NULL DEFAULT 'RMC Staff',
    staff_portal_subtitle VARCHAR(120) DEFAULT 'Booking portal',
    logo_url VARCHAR(512),
    favicon_url VARCHAR(512),
    font_family VARCHAR(120) NOT NULL DEFAULT 'Geist Variable',
    primary_color VARCHAR(32) NOT NULL DEFAULT '#1a1a1a',
    primary_foreground_color VARCHAR(32) NOT NULL DEFAULT '#fafafa',
    secondary_color VARCHAR(32) NOT NULL DEFAULT '#f4f4f5',
    secondary_foreground_color VARCHAR(32) NOT NULL DEFAULT '#1a1a1a',
    accent_color VARCHAR(32) NOT NULL DEFAULT '#f4f4f5',
    accent_foreground_color VARCHAR(32) NOT NULL DEFAULT '#1a1a1a',
    border_color VARCHAR(32) NOT NULL DEFAULT '#e4e4e7',
    ring_color VARCHAR(32) NOT NULL DEFAULT '#a1a1aa',
    destructive_color VARCHAR(32) NOT NULL DEFAULT '#dc2626',
    updated_at TIMESTAMP NULL,
    updated_by BIGINT NULL
);

INSERT INTO branding_config (company_name) VALUES ('RMC Booking');

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
VALUES ('branding', 'Branding', '/staff/branding', 'palette', JSON_ARRAY('ADMIN'), 35, TRUE, NULL);
