INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'rooms-rate-plans',
    'Rate plans',
    '/staff/rooms/rate-plans',
    'bed',
    allowed_roles,
    12,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'rooms'
  AND parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'rooms-rate-plans');
