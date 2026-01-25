-- V001__initial_setup.sql
-- Description: Initial database setup - extensions, functions, and common utilities
-- Author: Database Agent
-- Date: 2026-01-25

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create function for auto-updating updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create enum types for status fields
CREATE TYPE requirement_status AS ENUM ('DRAFT', 'ACTIVE', 'ARCHIVED', 'DELETED');
CREATE TYPE api_spec_status AS ENUM ('ACTIVE', 'DEPRECATED', 'ARCHIVED');
CREATE TYPE test_suite_status AS ENUM ('ACTIVE', 'DISABLED', 'ARCHIVED');
CREATE TYPE scenario_status AS ENUM ('ACTIVE', 'DISABLED', 'DELETED');
CREATE TYPE scenario_source AS ENUM ('AI_GENERATED', 'MANUAL', 'IMPORTED');
CREATE TYPE run_status AS ENUM ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED');
CREATE TYPE step_result_status AS ENUM ('PENDING', 'RUNNING', 'PASSED', 'FAILED', 'SKIPPED', 'ERROR');
CREATE TYPE qa_package_status AS ENUM ('REQUESTED', 'SPEC_FETCHED', 'GENERATING', 'EXECUTING', 'EVALUATING', 'COMPLETED', 'FAILED');
CREATE TYPE ai_log_status AS ENUM ('SUCCESS', 'FAILED', 'TIMEOUT', 'RATE_LIMITED');
CREATE TYPE test_mode AS ENUM ('STANDARD', 'SECURITY', 'PERFORMANCE');

-- Add comments for documentation
COMMENT ON FUNCTION update_updated_at_column() IS 'Trigger function to automatically update updated_at column on row modification';
