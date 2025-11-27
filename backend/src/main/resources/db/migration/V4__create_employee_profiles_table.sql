-- V4: Create employee_profiles table
-- Description: Creates the employee_profiles table with all fields classified as
--              SYSTEM_MANAGED, NON_SENSITIVE, or SENSITIVE (see PRD Section 3.2)

CREATE TABLE IF NOT EXISTS employee_profiles (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign key to users table
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,

    -- ============================================
    -- SYSTEM-MANAGED FIELDS
    -- Always read-only, managed by HR/IT systems
    -- ============================================
    legal_first_name VARCHAR(100) NOT NULL,
    legal_last_name VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    job_code VARCHAR(50),
    job_family VARCHAR(100),
    job_level VARCHAR(50),
    employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    hire_date DATE NOT NULL,
    termination_date DATE,
    fte NUMERIC(3, 2) DEFAULT 1.00, -- Full-Time Equivalent

    -- ============================================
    -- NON-SENSITIVE FIELDS
    -- Visible to everyone, editable by employee + manager
    -- ============================================
    preferred_name VARCHAR(100),
    job_title VARCHAR(150),
    office_location VARCHAR(200),
    work_phone VARCHAR(20),
    work_location_type VARCHAR(20), -- REMOTE, HYBRID, ONSITE
    bio TEXT,
    skills TEXT, -- Comma-separated or JSON
    profile_photo_url VARCHAR(500),

    -- ============================================
    -- SENSITIVE FIELDS
    -- Visible to employee + manager only, editable by employee only
    -- ============================================
    personal_email VARCHAR(255),
    personal_phone VARCHAR(20),
    home_address TEXT,
    emergency_contact_name VARCHAR(200),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relationship VARCHAR(50),
    date_of_birth DATE,
    visa_work_permit VARCHAR(200),
    absence_balance_days NUMERIC(5, 2),
    salary NUMERIC(12, 2),
    performance_rating VARCHAR(50),

    -- ============================================
    -- AUDIT FIELDS
    -- ============================================
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_fte CHECK (fte > 0 AND fte <= 1.00),
    CONSTRAINT chk_employment_status CHECK (employment_status IN ('ACTIVE', 'ON_LEAVE', 'TERMINATED', 'RETIRED')),
    CONSTRAINT chk_work_location_type CHECK (work_location_type IS NULL OR work_location_type IN ('REMOTE', 'HYBRID', 'ONSITE'))
);

-- Create index on user_id for fast lookups
CREATE INDEX IF NOT EXISTS idx_employee_profiles_user_id ON employee_profiles(user_id);

-- Create index on employment_status for filtering active employees
CREATE INDEX IF NOT EXISTS idx_employee_profiles_employment_status ON employee_profiles(employment_status);

-- Add comment to table
COMMENT ON TABLE employee_profiles IS 'Employee profile data with field classification (SYSTEM_MANAGED, NON_SENSITIVE, SENSITIVE)';
