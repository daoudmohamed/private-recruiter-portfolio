-- V1: Create adherent-related tables
-- Adhérents (members) and their beneficiaries

-- Adherents table
CREATE TABLE IF NOT EXISTS adherents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_adherent VARCHAR(20) UNIQUE NOT NULL,
    civilite VARCHAR(10) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE NOT NULL,
    street VARCHAR(255) NOT NULL,
    additional_info VARCHAR(255),
    postal_code VARCHAR(10) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(50) DEFAULT 'France',
    preference_contact VARCHAR(20) DEFAULT 'EMAIL',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create index for common lookups
CREATE INDEX idx_adherents_numero ON adherents(numero_adherent);
CREATE INDEX idx_adherents_email ON adherents(email);
CREATE INDEX idx_adherents_name ON adherents(last_name, first_name);

-- Beneficiaries table (spouse, children, partners)
CREATE TABLE IF NOT EXISTS beneficiaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adherent_id UUID NOT NULL REFERENCES adherents(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    relationship VARCHAR(20) NOT NULL,
    social_security_number VARCHAR(15),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_beneficiaries_adherent ON beneficiaries(adherent_id);

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_adherents_updated_at
    BEFORE UPDATE ON adherents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE adherents IS 'Health insurance members (adhérents)';
COMMENT ON TABLE beneficiaries IS 'Beneficiaries linked to adherent contracts';
COMMENT ON COLUMN adherents.numero_adherent IS 'Unique member identification number (format: ADH-XXXXX)';
COMMENT ON COLUMN adherents.civilite IS 'Title: M, MME, MLLE';
COMMENT ON COLUMN adherents.preference_contact IS 'Preferred contact method: EMAIL, SMS, COURRIER, TELEPHONE';
COMMENT ON COLUMN beneficiaries.relationship IS 'Relationship to adherent: SPOUSE, CHILD, PARTNER';
