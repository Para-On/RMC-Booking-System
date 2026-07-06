-- Promote existing manager accounts to administrators (previous manager = platform admin).
UPDATE staff_user SET role = 'ADMIN' WHERE role = 'MANAGER';

-- Dev admin account: admin@rmc.local / manager123 (same hash as legacy manager seed).
INSERT INTO staff_user (email, password_hash, full_name, role)
VALUES (
    'admin@rmc.local',
    '$2a$10$FEWSKkUdLu7ELyBECRBHTONifG7.wPcG9Zm78ye2txxC7sIm3cgue',
    'System Admin',
    'ADMIN'
)
ON DUPLICATE KEY UPDATE role = 'ADMIN';

-- Platform administration modules: admin only.
UPDATE staff_nav_module
SET allowed_roles = JSON_ARRAY('ADMIN')
WHERE module_key IN ('settings', 'users', 'modules');

-- Day-to-day operational modules: all staff roles.
UPDATE staff_nav_module
SET allowed_roles = JSON_ARRAY('FRONT_DESK', 'MANAGER', 'ADMIN')
WHERE module_key IN ('dashboard', 'arrivals');

-- Rooms modules: managers get these by default; admins always have access.
UPDATE staff_nav_module
SET allowed_roles = JSON_ARRAY('MANAGER', 'ADMIN')
WHERE module_key IN ('rooms', 'rooms-catalog', 'rooms-config', 'rooms-operations');
