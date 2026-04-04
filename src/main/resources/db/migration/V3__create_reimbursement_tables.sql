-- V3: Create reimbursement-related tables
-- Reimbursement claims and their tracking

-- Reimbursements table
CREATE TABLE IF NOT EXISTS reimbursements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adherent_id UUID NOT NULL REFERENCES adherents(id),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    beneficiary_id UUID REFERENCES beneficiaries(id),
    beneficiary_name VARCHAR(200),
    care_date DATE NOT NULL,
    submission_date DATE NOT NULL,
    processing_date DATE,
    payment_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    care_type VARCHAR(50) NOT NULL,
    care_code VARCHAR(20),
    care_label VARCHAR(500) NOT NULL,
    provider_name VARCHAR(200),
    provider_code VARCHAR(20),
    amount_claimed DECIMAL(10, 2) NOT NULL,
    secu_base DECIMAL(10, 2),
    secu_amount DECIMAL(10, 2),
    mutuelle_amount DECIMAL(10, 2),
    total_reimbursed DECIMAL(10, 2),
    remaining_charge DECIMAL(10, 2),
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_reimbursements_adherent ON reimbursements(adherent_id);
CREATE INDEX idx_reimbursements_contract ON reimbursements(contract_id);
CREATE INDEX idx_reimbursements_status ON reimbursements(status);
CREATE INDEX idx_reimbursements_care_date ON reimbursements(care_date DESC);
CREATE INDEX idx_reimbursements_submission_date ON reimbursements(submission_date DESC);

-- Reimbursement documents (links to uploaded files)
CREATE TABLE IF NOT EXISTS reimbursement_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reimbursement_id UUID NOT NULL REFERENCES reimbursements(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_reimbursement_docs_reimbursement ON reimbursement_documents(reimbursement_id);

-- Consumption tracking (for ceilings/plafonds)
CREATE TABLE IF NOT EXISTS consumption_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adherent_id UUID NOT NULL REFERENCES adherents(id),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    category VARCHAR(30) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    ceiling DECIMAL(10, 2) NOT NULL,
    consumed DECIMAL(10, 2) NOT NULL DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(adherent_id, contract_id, category, period_start)
);

CREATE INDEX idx_consumption_adherent ON consumption_tracking(adherent_id);
CREATE INDEX idx_consumption_period ON consumption_tracking(period_start, period_end);

-- Trigger for reimbursements updated_at
CREATE TRIGGER update_reimbursements_updated_at
    BEFORE UPDATE ON reimbursements
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments
COMMENT ON TABLE reimbursements IS 'Reimbursement claims from adherents';
COMMENT ON TABLE reimbursement_documents IS 'Documents attached to reimbursement claims';
COMMENT ON TABLE consumption_tracking IS 'Tracks consumption against ceilings for guarantees';
COMMENT ON COLUMN reimbursements.status IS 'Status: SUBMITTED, PROCESSING, COMPLETED, REJECTED, PENDING_INFO';
COMMENT ON COLUMN reimbursements.care_type IS 'Type of care: CONSULTATION_GENERALISTE, OPTIQUE, DENTAIRE, etc.';
COMMENT ON COLUMN reimbursements.secu_base IS 'Base amount used by Social Security for calculation';
COMMENT ON COLUMN reimbursements.secu_amount IS 'Amount reimbursed by Social Security';
COMMENT ON COLUMN reimbursements.mutuelle_amount IS 'Amount reimbursed by the mutuelle';
COMMENT ON COLUMN reimbursement_documents.document_type IS 'Type: FACTURE, ORDONNANCE, DECOMPTE_SECU';
