-- Seed demo users for testing
-- Password for all users: 'password123' (BCrypt hashed)
INSERT INTO users (id, employee_id, email, password, manager_id, role) VALUES
-- Manager
('f47ac10b-58cc-4372-a567-0e02b2c3d479', 'MGR001', 'manager@company.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'MANAGER'),

-- Employee 1 (reports to manager)
('c9b7e5d2-8f3a-4d1c-9e6b-2a4f7c8d1e3f', 'EMP001', 'emp1@company.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 'EMPLOYEE'),

-- Employee 2 (reports to manager)
('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 'EMP002', 'emp2@company.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 'EMPLOYEE');
