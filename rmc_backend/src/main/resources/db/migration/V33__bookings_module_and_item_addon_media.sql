-- Bookings parent module + move All bookings out of Arrivals
INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'bookings',
    'Bookings',
    '#',
    'layout',
    allowed_roles,
    12,
    enabled,
    NULL
FROM staff_nav_module
WHERE module_key = 'arrivals'
  AND parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'bookings');

UPDATE staff_nav_module child
JOIN staff_nav_module parent ON parent.module_key = 'bookings' AND parent.parent_id IS NULL
SET
    child.module_key = 'bookings-all',
    child.label = 'All bookings',
    child.path = '/staff/bookings',
    child.icon = 'layout',
    child.parent_id = parent.id,
    child.sort_order = 10
WHERE child.module_key = 'arrivals-bookings';

INSERT INTO staff_nav_module (module_key, label, path, icon, allowed_roles, sort_order, enabled, parent_id)
SELECT
    'bookings-all',
    'All bookings',
    '/staff/bookings',
    'layout',
    parent.allowed_roles,
    10,
    parent.enabled,
    parent.id
FROM staff_nav_module parent
WHERE parent.module_key = 'bookings'
  AND parent.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM staff_nav_module WHERE module_key = 'bookings-all');

-- Item add-ons: photo + optional free/paid pricing (parity with services)
ALTER TABLE room_item_addon
    ADD COLUMN subtitle VARCHAR(200) NULL AFTER name,
    ADD COLUMN details TEXT NULL AFTER subtitle,
    ADD COLUMN image_url VARCHAR(512) NULL AFTER details,
    ADD COLUMN is_free BOOLEAN NOT NULL DEFAULT TRUE AFTER image_url;

UPDATE room_item_addon
SET is_free = CASE WHEN price IS NULL OR price <= 0 THEN TRUE ELSE FALSE END;
