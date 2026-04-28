-- Seed admin passenger account (password: "secret", BCrypt 10 rounds)
INSERT INTO passengers (name, email, phone, password_hash)
VALUES (
    'Admin',
    'admin@taxi.com',
    '+70000000000',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
)
ON CONFLICT (email) DO NOTHING;
