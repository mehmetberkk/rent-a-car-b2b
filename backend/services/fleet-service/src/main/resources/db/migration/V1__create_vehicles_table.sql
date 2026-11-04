CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL,
    plate VARCHAR(15) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    model_year INTEGER NOT NULL,
    fuel_type VARCHAR(20) NOT NULL,
    transmission VARCHAR(20) NOT NULL,
    daily_pool_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_agency_plate UNIQUE (agency_id, plate),
    CONSTRAINT chk_fuel_type CHECK (fuel_type IN ('GASOLINE', 'DIESEL', 'ELECTRIC', 'HYBRID')),
    CONSTRAINT chk_transmission CHECK (transmission IN ('MANUAL', 'AUTOMATIC')),
    CONSTRAINT chk_vehicle_status CHECK (status IN ('AVAILABLE', 'RENTED', 'IN_POOL')),
    CONSTRAINT chk_daily_pool_price CHECK (daily_pool_price > 0)
);

CREATE INDEX idx_vehicles_agency_id ON vehicles(agency_id);
CREATE INDEX idx_vehicles_status ON vehicles(status);

