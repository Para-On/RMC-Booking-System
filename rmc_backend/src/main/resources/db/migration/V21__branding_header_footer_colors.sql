ALTER TABLE branding_config
    ADD COLUMN header_background_color VARCHAR(32) NOT NULL DEFAULT '#0f172a',
    ADD COLUMN header_foreground_color VARCHAR(32) NOT NULL DEFAULT '#f8fafc',
    ADD COLUMN footer_background_color VARCHAR(32) NOT NULL DEFAULT '#0f172a',
    ADD COLUMN footer_foreground_color VARCHAR(32) NOT NULL DEFAULT '#f8fafc';
