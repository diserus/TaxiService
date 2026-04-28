-- Seed admin passenger account (password: "secret", BCrypt 10 rounds)
INSERT INTO passengers (name, email, phone, password_hash)
VALUES (
    'Admin',
    'admin@taxi.com',
    '+70000000000',
    '$2a$10$YYh.EKPirKXbk2/QE.pZ9um/Ua8BXzCUuYLcHmtYr6rhuRpTIuupS'
)
ON CONFLICT (email) DO NOTHING;
