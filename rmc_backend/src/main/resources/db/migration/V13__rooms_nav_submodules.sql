UPDATE staff_nav_module
SET path = '#',
    label = 'Rooms'
WHERE module_key = 'rooms';

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'rooms-catalog',
    'Create room',
    '/staff/rooms/catalog',
    'bed',
    allowed_roles,
    10,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'rooms';

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'rooms-operations',
    'View and update room',
    '/staff/rooms/operations',
    'bed',
    allowed_roles,
    20,
    enabled,
    id
FROM staff_nav_module
WHERE module_key = 'rooms';
