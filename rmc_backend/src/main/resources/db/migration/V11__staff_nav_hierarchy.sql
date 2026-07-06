ALTER TABLE staff_nav_module
    ADD COLUMN parent_id BIGINT NULL,
    ADD COLUMN allowed_roles JSON NULL,
    ADD CONSTRAINT fk_staff_nav_parent FOREIGN KEY (parent_id) REFERENCES staff_nav_module (id) ON DELETE CASCADE;

UPDATE staff_nav_module SET allowed_roles = JSON_ARRAY(required_role);

ALTER TABLE staff_nav_module DROP COLUMN required_role;

CREATE INDEX idx_staff_nav_parent ON staff_nav_module (parent_id, sort_order);
