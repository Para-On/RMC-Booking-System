CREATE TABLE staff_nav_module (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    module_key     VARCHAR(50) NOT NULL UNIQUE,
    label          VARCHAR(100) NOT NULL,
    path           VARCHAR(200) NOT NULL,
    icon           VARCHAR(50) NOT NULL,
    required_role  VARCHAR(20) NOT NULL DEFAULT 'FRONT_DESK',
    sort_order     INT NOT NULL DEFAULT 0,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO staff_nav_module (module_key, label, path, icon, required_role, sort_order, enabled) VALUES
('arrivals', 'Arrivals', '/staff/arrivals', 'calendar', 'FRONT_DESK', 10, TRUE),
('rooms', 'Rooms', '/staff/rooms', 'bed', 'MANAGER', 20, TRUE),
('settings', 'Settings', '/staff/settings', 'settings', 'MANAGER', 30, TRUE),
('users', 'Staff users', '/staff/users', 'users', 'MANAGER', 40, TRUE),
('modules', 'Sidebar modules', '/staff/modules', 'layout', 'MANAGER', 50, TRUE);
