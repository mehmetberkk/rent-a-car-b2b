CREATE TABLE IF NOT EXISTS agency_projections (
    agency_id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    contact_person VARCHAR(200) NOT NULL,
    address TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

