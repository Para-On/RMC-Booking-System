INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'arrivals-guests',
    'Guests',
    '/staff/guests',
    'users',
    allowed_roles,
    30,
    TRUE,
    id
FROM staff_nav_module
WHERE module_key = 'arrivals'
  AND parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'arrivals-guests');
