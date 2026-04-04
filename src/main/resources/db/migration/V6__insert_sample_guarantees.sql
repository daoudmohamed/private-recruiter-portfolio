-- V6: Insert sample guarantees data
-- Reference data for the guarantee catalog

-- Insert sample guarantees for different categories

-- Hospitalisation
INSERT INTO guarantees (id, code, name, description, category, coverage_percentage, ceiling, waiting_period_days) VALUES
(gen_random_uuid(), 'HOSPI-100', 'Hospitalisation 100%', 'Frais de séjour remboursés à 100% de la base Sécu', 'HOSPITALISATION', 100, NULL, 0),
(gen_random_uuid(), 'HOSPI-150', 'Hospitalisation 150%', 'Frais de séjour remboursés à 150% de la base Sécu', 'HOSPITALISATION', 150, NULL, 0),
(gen_random_uuid(), 'HOSPI-200', 'Hospitalisation 200%', 'Frais de séjour remboursés à 200% de la base Sécu', 'HOSPITALISATION', 200, NULL, 0),
(gen_random_uuid(), 'CHAMBRE-PART', 'Chambre particulière', 'Forfait journalier chambre particulière', 'HOSPITALISATION', 100, 80.00, 0);

-- Optique
INSERT INTO guarantees (id, code, name, description, category, coverage_percentage, ceiling, frequency, waiting_period_days) VALUES
(gen_random_uuid(), 'OPT-MONTURE-100', 'Monture 100€', 'Remboursement monture jusqu''à 100€', 'OPTIQUE', 100, 100.00, 'BIENNIAL', 90),
(gen_random_uuid(), 'OPT-MONTURE-150', 'Monture 150€', 'Remboursement monture jusqu''à 150€', 'OPTIQUE', 100, 150.00, 'BIENNIAL', 90),
(gen_random_uuid(), 'OPT-VERRES-SIMPLES', 'Verres simples', 'Remboursement verres simples', 'OPTIQUE', 100, 100.00, 'BIENNIAL', 90),
(gen_random_uuid(), 'OPT-VERRES-COMPLEXES', 'Verres complexes', 'Remboursement verres complexes', 'OPTIQUE', 100, 200.00, 'BIENNIAL', 90),
(gen_random_uuid(), 'OPT-LENTILLES', 'Lentilles', 'Forfait annuel lentilles', 'OPTIQUE', 100, 150.00, 'ANNUAL', 90);

-- Dentaire
INSERT INTO guarantees (id, code, name, description, category, coverage_percentage, ceiling, waiting_period_days) VALUES
(gen_random_uuid(), 'DENT-SOINS-100', 'Soins dentaires 100%', 'Soins conservateurs remboursés à 100%', 'DENTAIRE', 100, NULL, 0),
(gen_random_uuid(), 'DENT-PROTHESES-125', 'Prothèses 125%', 'Prothèses dentaires remboursées à 125%', 'DENTAIRE', 125, NULL, 180),
(gen_random_uuid(), 'DENT-PROTHESES-200', 'Prothèses 200%', 'Prothèses dentaires remboursées à 200%', 'DENTAIRE', 200, NULL, 180),
(gen_random_uuid(), 'DENT-ORTHO', 'Orthodontie', 'Forfait annuel orthodontie', 'DENTAIRE', 100, 500.00, 180),
(gen_random_uuid(), 'DENT-IMPLANT', 'Implants dentaires', 'Forfait par implant', 'DENTAIRE', 100, 400.00, 365);

-- Médecine générale
INSERT INTO guarantees (id, code, name, description, category, coverage_percentage, ceiling, waiting_period_days) VALUES
(gen_random_uuid(), 'MED-CONSULT-100', 'Consultations 100%', 'Consultations remboursées à 100%', 'MEDECINE_GENERALE', 100, NULL, 0),
(gen_random_uuid(), 'MED-CONSULT-150', 'Consultations 150%', 'Consultations remboursées à 150% (dépassements)', 'MEDECINE_GENERALE', 150, NULL, 0),
(gen_random_uuid(), 'MED-ANALYSES', 'Analyses médicales', 'Analyses et examens de laboratoire', 'MEDECINE_GENERALE', 100, NULL, 0),
(gen_random_uuid(), 'MED-RADIO', 'Radiologie', 'Examens radiologiques', 'MEDECINE_GENERALE', 100, NULL, 0);

-- Pharmacie
INSERT INTO guarantees (id, code, name, description, category, coverage_percentage, ceiling, waiting_period_days) VALUES
(gen_random_uuid(), 'PHARMA-100', 'Pharmacie 100%', 'Médicaments remboursés à 100%', 'PHARMACIE', 100, NULL, 0);

-- Auxiliaires médicaux
INSERT INTO guarantees (id, code, name, description, category, coverage_percentage, ceiling, waiting_period_days) VALUES
(gen_random_uuid(), 'AUX-KINE', 'Kinésithérapie', 'Séances de kinésithérapie', 'AUXILIAIRES_MEDICAUX', 100, NULL, 0),
(gen_random_uuid(), 'AUX-ORTHO', 'Orthophonie', 'Séances d''orthophonie', 'AUXILIAIRES_MEDICAUX', 100, NULL, 0),
(gen_random_uuid(), 'AUX-INFIRMIER', 'Soins infirmiers', 'Soins infirmiers', 'AUXILIAIRES_MEDICAUX', 100, NULL, 0);

-- Bien-être (médecines douces)
INSERT INTO guarantees (id, code, name, description, category, coverage_percentage, ceiling, frequency, waiting_period_days) VALUES
(gen_random_uuid(), 'BIEN-OSTEO', 'Ostéopathie', 'Forfait annuel ostéopathie', 'BIEN_ETRE', 100, 150.00, 'ANNUAL', 90),
(gen_random_uuid(), 'BIEN-ACU', 'Acupuncture', 'Forfait annuel acupuncture', 'BIEN_ETRE', 100, 100.00, 'ANNUAL', 90);

-- Prévention
INSERT INTO guarantees (id, code, name, description, category, coverage_percentage, ceiling, waiting_period_days) VALUES
(gen_random_uuid(), 'PREV-VACCIN', 'Vaccins non remboursés', 'Vaccins non pris en charge par la Sécu', 'PREVENTION', 100, 100.00, 0),
(gen_random_uuid(), 'PREV-DEPISTAGE', 'Dépistages', 'Actes de dépistage', 'PREVENTION', 100, NULL, 0);

-- Comments
COMMENT ON TABLE guarantees IS 'Reference data: available guarantees/coverages by category';
