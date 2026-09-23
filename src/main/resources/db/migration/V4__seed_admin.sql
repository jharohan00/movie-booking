-- V4: Default admin user
-- BCrypt hash of 'Admin@123' (strength 10)
INSERT INTO users (email, password, full_name, role)
VALUES ('admin@moviebooking.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'System Admin',
        'ADMIN');
