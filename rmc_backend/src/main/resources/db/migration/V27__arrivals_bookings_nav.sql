UPDATE staff_nav_module
SET path = '#',
    label = 'Arrivals'
WHERE module_key = 'arrivals'
  AND path <> '#';

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'arrivals-today',
    'Today''s arrivals',
    '/staff/arrivals',
    'calendar',
    allowed_roles,
    10,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'arrivals'
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'arrivals-today');

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'arrivals-bookings',
    'All bookings',
    '/staff/arrivals/bookings',
    'layout',
    allowed_roles,
    20,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'arrivals'
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'arrivals-bookings');
