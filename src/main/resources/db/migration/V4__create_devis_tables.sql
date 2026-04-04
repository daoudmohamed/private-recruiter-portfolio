-- V4: Create devis (quote) tables
-- Quotes submitted for coverage estimation

-- Devis table
CREATE TABLE IF NOT EXISTS devis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adherent_id UUID NOT NULL REFERENCES adherents(id),
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    valid_until DATE NOT NULL,
    practitioner_name VARCHAR(200),
    practitioner_specialty VARCHAR(100),
    practitioner_address TEXT,
    practitioner_phone VARCHAR(20),
    practitioner_is_partner BOOLEAN DEFAULT FALSE,
    total_estimated DECIMAL(10, 2) NOT NULL,
    secu_estimate DECIMAL(10, 2) NOT NULL,
    mutuelle_estimate DECIMAL(10, 2) NOT NULL,
    remaining_charge DECIMAL(10, 2) NOT NULL,
    notes TEXT,
    document_id UUID
);

CREATE INDEX idx_devis_adherent ON devis(adherent_id);
CREATE INDEX idx_devis_status ON devis(status);
CREATE INDEX idx_devis_type ON devis(type);
CREATE INDEX idx_devis_valid_until ON devis(valid_until);

-- Devis items (line items in a quote)
CREATE TABLE IF NOT EXISTS devis_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    devis_id UUID NOT NULL REFERENCES devis(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    code_acte VARCHAR(20),
    quantity INTEGER DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    secu_base DECIMAL(10, 2),
    secu_estimate DECIMAL(10, 2) NOT NULL,
    mutuelle_estimate DECIMAL(10, 2) NOT NULL,
    remaining_charge DECIMAL(10, 2) NOT NULL
);

CREATE INDEX idx_devis_items_devis ON devis_items(devis_id);

-- Comments
COMMENT ON TABLE devis IS 'Quotes submitted by adherents for coverage estimation';
COMMENT ON TABLE devis_items IS 'Line items within a quote';
COMMENT ON COLUMN devis.type IS 'Quote type: OPTIQUE, DENTAIRE, HOSPITALISATION, APPAREILLAGE, AUDIOPROTHESE';
COMMENT ON COLUMN devis.status IS 'Status: PENDING, VALIDATED, REJECTED, EXPIRED, USED';
COMMENT ON COLUMN devis.practitioner_is_partner IS 'Whether the practitioner is a partner of the mutuelle';
COMMENT ON COLUMN devis_items.code_acte IS 'Healthcare act code (CCAM, NGAP)';
