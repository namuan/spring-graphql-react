-- H2-compatible seed data (same catalog as Flyway V2__seed_vehicle_catalog.sql).
-- Runs after Hibernate creates the schema because of
-- spring.jpa.defer-datasource-initialization=true in the test profile.

INSERT INTO vehicle_model (id, brand, name, model_year, base_price_cents, description,
                           engine, power_ps, acceleration_s, top_speed_kph, drivetrain, range_km, seats) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Aster', 'Vale', 2026, 4590000,
     'The Aster Vale is a sleek grand tourer with adaptive air suspension and a driver-focused cockpit.',
     'Twin-turbo V6', 520, 3.9, 300, 'AWD', NULL, 4),
    ('00000000-0000-0000-0000-000000000002', 'Aster', 'Terra', 2026, 5290000,
     'The Aster Terra is a rugged all-terrain vehicle engineered for exploration beyond the tarmac.',
     'Turbo diesel V6', 410, 5.2, 250, 'AWD', NULL, 5);

INSERT INTO trim (id, model_id, name, price_delta_cents) VALUES
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001', 'Touring', 0),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000001', 'Apex', 610000),
    ('00000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000002', 'Expedition', 0),
    ('00000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000002', 'Pioneer', 520000);

INSERT INTO vehicle_option (id, model_id, name, category, price_cents) VALUES
    ('00000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000001', 'Panoramic canopy', 'EXTERIOR', 145000),
    ('00000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000001', 'Velocity package', 'PERFORMANCE', 265000),
    ('00000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000001', 'Comfort seats', 'COMFORT', 95000),
    ('00000000-0000-0000-0000-000000000024', '00000000-0000-0000-0000-000000000001', 'Infotainment pro', 'TECHNOLOGY', 175000),
    ('00000000-0000-0000-0000-000000000025', '00000000-0000-0000-0000-000000000002', 'Velocity package', 'PERFORMANCE', 295000),
    ('00000000-0000-0000-0000-000000000026', '00000000-0000-0000-0000-000000000002', 'Rough terrain kit', 'EXTERIOR', 210000),
    ('00000000-0000-0000-0000-000000000027', '00000000-0000-0000-0000-000000000002', 'Climate package', 'COMFORT', 110000),
    ('00000000-0000-0000-0000-000000000028', '00000000-0000-0000-0000-000000000002', 'Off-road camera system', 'TECHNOLOGY', 165000);
