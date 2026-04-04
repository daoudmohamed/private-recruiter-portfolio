-- V5: Create document tables
-- Documents generated for or uploaded by adherents

-- Documents table
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adherent_id UUID NOT NULL REFERENCES adherents(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    valid_from DATE,
    valid_until DATE,
    metadata JSONB DEFAULT '{}'::JSONB
);

CREATE INDEX idx_documents_adherent ON documents(adherent_id);
CREATE INDEX idx_documents_type ON documents(type);
CREATE INDEX idx_documents_valid_until ON documents(valid_until);

-- Document generation requests (for async generation)
CREATE TABLE IF NOT EXISTS document_generation_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adherent_id UUID NOT NULL REFERENCES adherents(id),
    document_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    parameters JSONB DEFAULT '{}'::JSONB,
    result_document_id UUID REFERENCES documents(id),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_doc_gen_requests_adherent ON document_generation_requests(adherent_id);
CREATE INDEX idx_doc_gen_requests_status ON document_generation_requests(status);

-- Comments
COMMENT ON TABLE documents IS 'Documents associated with adherents (cards, attestations, etc.)';
COMMENT ON TABLE document_generation_requests IS 'Async document generation requests';
COMMENT ON COLUMN documents.type IS 'Type: CARTE_TIERS_PAYANT, ATTESTATION_DROITS, RELEVE_PRESTATIONS, etc.';
COMMENT ON COLUMN documents.metadata IS 'Additional metadata as JSON';
COMMENT ON COLUMN document_generation_requests.status IS 'Status: QUEUED, PROCESSING, COMPLETED, FAILED';
