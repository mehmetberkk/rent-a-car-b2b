CREATE TABLE agencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    contact_person VARCHAR(200) NOT NULL,
    address TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_agencies_owner_id ON agencies(owner_id);
CREATE INDEX idx_agencies_city ON agencies(city);

