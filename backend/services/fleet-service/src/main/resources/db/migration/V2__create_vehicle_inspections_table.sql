CREATE TABLE vehicle_inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL UNIQUE REFERENCES vehicles(id) ON DELETE CASCADE,
    traffic_insurance_expiry DATE,
    casco_expiry DATE,
    inspection_expiry DATE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inspections_dates ON vehicle_inspections(traffic_insurance_expiry, casco_expiry, inspection_expiry);

