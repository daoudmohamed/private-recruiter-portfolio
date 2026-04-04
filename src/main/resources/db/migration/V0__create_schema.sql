-- Create the mutuelle schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS mutuelle;

-- Grant permissions to mutuelle user
GRANT ALL PRIVILEGES ON SCHEMA mutuelle TO mutuelle;

-- Set search path for all subsequent migrations
-- This ensures all tables are created in the mutuelle schema
ALTER DATABASE mutuelle SET search_path = mutuelle, public;
