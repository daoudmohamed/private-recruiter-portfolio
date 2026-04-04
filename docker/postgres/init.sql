-- Initial PostgreSQL setup for Mutuelle RAG Agent
-- This script runs on first container startup

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create schema
CREATE SCHEMA IF NOT EXISTS mutuelle;

-- Grant privileges
GRANT ALL PRIVILEGES ON SCHEMA mutuelle TO mutuelle;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA mutuelle TO mutuelle;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA mutuelle TO mutuelle;

-- Set default search path
ALTER DATABASE mutuelle SET search_path TO mutuelle, public;
