ALTER TABLE staff_user
    ADD COLUMN profile_image_url VARCHAR(512) NULL AFTER full_name,
    ADD COLUMN phone VARCHAR(40) NULL AFTER profile_image_url,
    ADD COLUMN theme_preference VARCHAR(10) NOT NULL DEFAULT 'LIGHT' AFTER phone;
