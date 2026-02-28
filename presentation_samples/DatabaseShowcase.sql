-- TODO(@dba_team priority:CRITICAL category:database issue:PROJ-103): Add an index to improve search query performance
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    
    -- TODO(priority:MEDIUM category:compliance risk:high due:2023-05-15): Ensure session_token is securely hashed (OVERDUE!)
    session_token VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* 
 * TODO(@data_eng priority:HIGH issue:PROJ-104): Set up automated vacuuming for this table
 */
