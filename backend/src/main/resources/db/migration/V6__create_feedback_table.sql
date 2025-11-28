-- V6: Create feedback table
-- Description: Creates feedback table for peer feedback between employees
--              Idempotent: Uses IF NOT EXISTS

-- Create feedback table
CREATE TABLE IF NOT EXISTS feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    text TEXT NOT NULL,
    ai_polished BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_feedback_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT chk_feedback_text_length CHECK (LENGTH(text) >= 1),
    CONSTRAINT chk_feedback_not_self CHECK (author_id != recipient_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_feedback_author_id ON feedback(author_id);
CREATE INDEX IF NOT EXISTS idx_feedback_recipient_id ON feedback(recipient_id);
CREATE INDEX IF NOT EXISTS idx_feedback_created_at ON feedback(created_at DESC);
