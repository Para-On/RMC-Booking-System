UPDATE staff_nav_module SET sort_order = sort_order + 10 WHERE parent_id IS NULL;

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
VALUES ('dashboard', 'Dashboard', '/staff/dashboard', 'layout', JSON_ARRAY('FRONT_DESK', 'MANAGER'), 5, TRUE, NULL);
