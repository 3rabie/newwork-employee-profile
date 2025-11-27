-- Seed demo users for testing (idempotent - only inserts if not exists)
-- Password for all users: 'pwd951753'
-- BCrypt hash generated with: new BCryptPasswordEncoder().encode("pwd951753")

-- Insert manager if not exists
INSERT INTO users (id, employee_id, email, password, manager_id, role)
SELECT 'f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, 'MGR001', 'manager@company.com',
       '$2a$12$YktM6ODfqOzX0bOardbHq.16AjW8UaKiuPE.nxzO8/G9mnS5xD4X2', NULL, 'MANAGER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'manager@company.com');

-- Insert employee 1 if not exists
INSERT INTO users (id, employee_id, email, password, manager_id, role)
SELECT 'c9b7e5d2-8f3a-4d1c-9e6b-2a4f7c8d1e3f'::uuid, 'EMP001', 'emp1@company.com',
       '$2a$12$YktM6ODfqOzX0bOardbHq.16AjW8UaKiuPE.nxzO8/G9mnS5xD4X2',
       'f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, 'EMPLOYEE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'emp1@company.com');

-- Insert employee 2 if not exists
INSERT INTO users (id, employee_id, email, password, manager_id, role)
SELECT 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d'::uuid, 'EMP002', 'emp2@company.com',
       '$2a$12$YktM6ODfqOzX0bOardbHq.16AjW8UaKiuPE.nxzO8/G9mnS5xD4X2',
       'f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, 'EMPLOYEE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'emp2@company.com');
