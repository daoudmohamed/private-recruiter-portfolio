-- V2: Create contract-related tables
-- Contracts, guarantees, and their relationships

-- Contracts table
CREATE TABLE IF NOT EXISTS contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_number VARCHAR(20) UNIQUE NOT NULL,
    adherent_id UUID NOT NULL REFERENCES adherents(id),
    type VARCHAR(20) NOT NULL,
    formula VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE NOT NULL,
    end_date DATE,
    monthly_premium DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_contracts_adherent ON contracts(adherent_id);
CREATE INDEX idx_contracts_number ON contracts(contract_number);
CREATE INDEX idx_contracts_status ON contracts(status);

-- Guarantees table (master list of all available guarantees)
CREATE TABLE IF NOT EXISTS guarantees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL,
    coverage_percentage INTEGER NOT NULL,
    ceiling DECIMAL(10, 2),
    frequency VARCHAR(20),
    waiting_period_days INTEGER DEFAULT 0
);

CREATE INDEX idx_guarantees_code ON guarantees(code);
CREATE INDEX idx_guarantees_category ON guarantees(category);

-- Contract-Guarantee relationship (many-to-many)
CREATE TABLE IF NOT EXISTS contract_guarantees (
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    guarantee_id UUID NOT NULL REFERENCES guarantees(id),
    custom_ceiling DECIMAL(10, 2),
    custom_percentage INTEGER,
    PRIMARY KEY (contract_id, guarantee_id)
);

CREATE INDEX idx_contract_guarantees_contract ON contract_guarantees(contract_id);

-- Contract options table
CREATE TABLE IF NOT EXISTS contract_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    option_code VARCHAR(30) NOT NULL,
    activated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(contract_id, option_code)
);

CREATE INDEX idx_contract_options_contract ON contract_options(contract_id);

-- Waiting periods (délais de carence)
CREATE TABLE IF NOT EXISTS waiting_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    guarantee_category VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration_months INTEGER NOT NULL,
    UNIQUE(contract_id, guarantee_category)
);

CREATE INDEX idx_waiting_periods_contract ON waiting_periods(contract_id);
CREATE INDEX idx_waiting_periods_end_date ON waiting_periods(end_date);

-- Trigger for contracts updated_at
CREATE TRIGGER update_contracts_updated_at
    BEFORE UPDATE ON contracts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments
COMMENT ON TABLE contracts IS 'Health insurance contracts';
COMMENT ON TABLE guarantees IS 'Master list of available guarantees/coverages';
COMMENT ON TABLE contract_guarantees IS 'Links contracts to their guarantees';
COMMENT ON TABLE contract_options IS 'Optional add-ons activated on contracts';
COMMENT ON TABLE waiting_periods IS 'Waiting periods (délais de carence) for specific guarantees';
COMMENT ON COLUMN contracts.type IS 'Contract type: INDIVIDUAL, FAMILY, COLLECTIVE';
COMMENT ON COLUMN contracts.formula IS 'Formula tier: ESSENTIELLE, CONFORT, PREMIUM';
COMMENT ON COLUMN contracts.status IS 'Contract status: ACTIVE, SUSPENDED, TERMINATED, PENDING';
COMMENT ON COLUMN guarantees.category IS 'Guarantee category: HOSPITALISATION, OPTIQUE, DENTAIRE, etc.';
COMMENT ON COLUMN guarantees.frequency IS 'Coverage renewal frequency: ANNUAL, BIENNIAL, TRIENNIAL';
