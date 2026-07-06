UPDATE staff_nav_module
SET allowed_roles = JSON_ARRAY('FRONT_DESK', 'MANAGER')
WHERE module_key = 'arrivals';
