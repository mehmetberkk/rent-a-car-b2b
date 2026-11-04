CREATE TABLE IF NOT EXISTS pool_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL UNIQUE,
    agency_id UUID NOT NULL,
    agency_name VARCHAR(200) NOT NULL,
    agency_city VARCHAR(100) NOT NULL,
    agency_phone VARCHAR(20) NOT NULL,
    contact_person VARCHAR(200) NOT NULL,
    agency_address TEXT NOT NULL,
    plate VARCHAR(15) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    model_year INTEGER NOT NULL,
    fuel_type VARCHAR(20) NOT NULL,
    transmission VARCHAR(20) NOT NULL,
    daily_pool_price DECIMAL(10,2) NOT NULL,
    listed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pool_listings_agency_id ON pool_listings(agency_id);
CREATE INDEX IF NOT EXISTS idx_pool_listings_city ON pool_listings(agency_city);
CREATE INDEX IF NOT EXISTS idx_pool_listings_brand ON pool_listings(brand);
CREATE INDEX IF NOT EXISTS idx_pool_listings_fuel_type ON pool_listings(fuel_type);
CREATE INDEX IF NOT EXISTS idx_pool_listings_transmission ON pool_listings(transmission);

