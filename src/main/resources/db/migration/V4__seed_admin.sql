-- V4: Default admin user
-- BCrypt(10) hash of the string: password
-- Change this before deploying to production!
INSERT INTO users (email, password, full_name, role)
VALUES ('admin@moviebooking.com',
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
        'System Admin',
        'ADMIN');
