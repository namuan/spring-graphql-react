CREATE TABLE vehicle_model (
    id UUID PRIMARY KEY,
    brand VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    model_year INTEGER NOT NULL,
    base_price_cents BIGINT NOT NULL CHECK (base_price_cents BETWEEN 0 AND 2147483647),
    description TEXT
);

CREATE TABLE trim (
    id UUID PRIMARY KEY,
    model_id UUID NOT NULL REFERENCES vehicle_model (id),
    name VARCHAR(100) NOT NULL,
    price_delta_cents BIGINT NOT NULL CHECK (price_delta_cents BETWEEN 0 AND 2147483647)
);

CREATE INDEX idx_trim_model_id ON trim (model_id);

CREATE TABLE vehicle_option (
    id UUID PRIMARY KEY,
    model_id UUID NOT NULL REFERENCES vehicle_model (id),
    name VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL,
    price_cents BIGINT NOT NULL CHECK (price_cents BETWEEN 0 AND 2147483647),
    CONSTRAINT chk_option_category CHECK (category IN ('PERFORMANCE', 'COMFORT', 'TECHNOLOGY', 'EXTERIOR'))
);

CREATE INDEX idx_vehicle_option_model_id ON vehicle_option (model_id);

CREATE TABLE vehicle_configuration (
    id UUID PRIMARY KEY,
    model_id UUID NOT NULL REFERENCES vehicle_model (id),
    trim_id UUID NOT NULL REFERENCES trim (id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL,
    total_price_cents BIGINT NOT NULL CHECK (total_price_cents BETWEEN 0 AND 2147483647)
);

CREATE INDEX idx_vehicle_configuration_model_id ON vehicle_configuration (model_id);

CREATE TABLE configuration_option (
    configuration_id UUID NOT NULL REFERENCES vehicle_configuration (id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES vehicle_option (id),
    PRIMARY KEY (configuration_id, option_id)
);
