INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'rooms-config',
    'Room configuration',
    '/staff/rooms/config',
    'settings',
    allowed_roles,
    5,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'rooms';
