-- V4: add technical specification data to each vehicle model.
--
-- Adds engine / performance / dimensions fields to vehicle_model and
-- populates them for every catalogue row (the two V2 "Aster" fixtures plus
-- the fifty V3 models). The (brand, name) pair is the stable key because V3
-- generates model UUIDs at runtime.
--
-- Notes:
--   * Columns are added nullable, populated, then locked down with NOT NULL
--     (range_km stays nullable: only battery-electric models report a range).
--   * A guard raises if any required spec is still missing, so the migration
--     fails loudly instead of leaving a half-populated catalogue.
--   * No label/value below contains the substrings "vale", "terra", "apex"
--     or "panoramic" (case-insensitive), keeping browser-test selectors
--     unambiguous.

ALTER TABLE vehicle_model
    ADD COLUMN engine VARCHAR(80),
    ADD COLUMN power_ps INTEGER,
    ADD COLUMN acceleration_s NUMERIC(4,1),
    ADD COLUMN top_speed_kph INTEGER,
    ADD COLUMN drivetrain VARCHAR(20),
    ADD COLUMN range_km INTEGER,
    ADD COLUMN seats INTEGER;

UPDATE vehicle_model m
SET engine         = s.engine,
    power_ps       = s.power_ps,
    acceleration_s = s.acceleration_s,
    top_speed_kph  = s.top_speed_kph,
    drivetrain     = s.drivetrain,
    range_km       = s.range_km,
    seats          = s.seats
FROM (VALUES
    ('Aster',    'Vale',          'Twin-turbo V6',      520,  3.9, 300, 'AWD', NULL, 4),
    ('Aster',    'Terra',         'Turbo diesel V6',    410,  5.2, 250, 'AWD', NULL, 5),
    ('Veloce',   'Nera GT',       'Twin-turbo V6',      560,  3.6, 310, 'RWD', NULL, 2),
    ('Veloce',   'Sera Coupe',    'Turbo V6',           480,  4.1, 285, 'RWD', NULL, 4),
    ('Veloce',   'Tigra Roadster','Turbo 2.0',          320,  4.6, 250, 'RWD', NULL, 2),
    ('Veloce',   'Mira Berlina',  'Turbo V6',           500,  4.0, 290, 'AWD', NULL, 5),
    ('Veloce',   'Zero EV',       'Dual electric motors',600, 3.0, 320, 'AWD', 520, 5),
    ('Veloce',   'Nova S',        'Turbo 2.0',          280,  5.4, 240, 'FWD', NULL, 5),
    ('Veloce',   'Cento',         'Twin-turbo V8',      640,  3.2, 330, 'AWD', NULL, 4),
    ('Orbis',    'Luxus',         'Turbo V6',           430,  4.8, 260, 'AWD', NULL, 5),
    ('Orbis',    'Vario',         'Turbo 2.0',          230,  7.2, 210, 'FWD', NULL, 5),
    ('Orbis',    'Kammer',        'Turbo 1.5',          190,  8.1, 205, 'FWD', NULL, 5),
    ('Orbis',    'Sturm',         'Twin-turbo V6',      470,  4.4, 270, 'AWD', NULL, 5),
    ('Orbis',    'Freya',         'Turbo 2.0',          240,  7.0, 215, 'FWD', NULL, 5),
    ('Orbis',    'Orion',         'Turbo V6',           400,  5.0, 255, 'RWD', NULL, 5),
    ('Orbis',    'Grenz',         'Turbo 2.0',          260,  6.2, 230, 'FWD', NULL, 5),
    ('Solstice', 'Ember',         'Turbo 2.0',          300,  4.9, 245, 'RWD', NULL, 2),
    ('Solstice', 'Crest',         'Twin-turbo V6',      510,  4.2, 280, 'RWD', NULL, 4),
    ('Solstice', 'Marlin',        'Turbo 1.8',          240,  5.9, 235, 'RWD', NULL, 2),
    ('Solstice', 'Pinnacle',      'Turbo V8',           580,  3.7, 300, 'RWD', NULL, 2),
    ('Solstice', 'Voyager',       'Turbo 2.0',          250,  6.8, 220, 'FWD', NULL, 7),
    ('Solstice', 'Tern',          'Turbo 1.8',          220,  6.4, 225, 'RWD', NULL, 2),
    ('Hajime',   'Kaze',          'Hybrid 1.8',         210,  7.4, 200, 'FWD', NULL, 5),
    ('Hajime',   'Yuki',          'Turbo 2.0',          290,  5.9, 225, 'AWD', NULL, 5),
    ('Hajime',   'Sora',          'Dual electric motors',380,  4.8, 220, 'AWD', 480, 5),
    ('Hajime',   'Hikari',        'Hybrid 1.8',         200,  7.8, 195, 'FWD', NULL, 5),
    ('Hajime',   'Sakura',        'Turbo 1.5',          170,  8.8, 185, 'FWD', NULL, 4),
    ('Hajime',   'Raiden',        'Dual electric motors',570,  3.1, 310, 'AWD', 460, 5),
    ('Auriga',   'Fjord',         'Turbo 2.0',          280,  6.0, 220, 'AWD', NULL, 5),
    ('Auriga',   'Polar',         'Turbo V6',           450,  4.9, 260, 'AWD', NULL, 7),
    ('Auriga',   'Boreal',        'Turbo 2.0',          250,  6.6, 215, 'AWD', NULL, 5),
    ('Auriga',   'Nival',         'Turbo 1.8',          210,  7.2, 200, 'AWD', NULL, 5),
    ('Auriga',   'Drift',         'Turbo 2.0',          320,  5.3, 240, 'AWD', NULL, 5),
    ('Auriga',   'Iskold',        'Hybrid 1.5',         190,  8.0, 190, 'FWD', NULL, 5),
    ('Cascadia', 'Rogue',         'Turbo 2.0',          300,  6.1, 210, 'AWD', NULL, 5),
    ('Cascadia', 'Canyon',        'Turbo diesel V6',    380,  6.8, 200, 'AWD', NULL, 5),
    ('Cascadia', 'Mesa',          'Turbo V6',           420,  6.0, 215, 'AWD', NULL, 7),
    ('Cascadia', 'Butte',         'Turbo 2.0',          260,  7.0, 200, 'AWD', NULL, 5),
    ('Cascadia', 'Summit',        'Twin-turbo V6',      500,  4.6, 250, 'AWD', NULL, 7),
    ('Cascadia', 'Aurora',        'Supercharged V8',    550,  4.0, 280, 'AWD', NULL, 4),
    ('Mirage',   'Lumine',        'Turbo 1.8',          230,  7.1, 210, 'FWD', NULL, 5),
    ('Mirage',   'Aria',          'Turbo 1.5',          200,  7.6, 205, 'FWD', NULL, 5),
    ('Mirage',   'Charme',        'Turbo 1.2',          150,  8.9, 180, 'FWD', NULL, 4),
    ('Mirage',   'Coeur',         'Turbo V6',           460,  4.5, 270, 'RWD', NULL, 4),
    ('Mirage',   'Vague',         'Hybrid 1.6',         220,  6.9, 205, 'FWD', NULL, 5),
    ('Mirage',   'Ciel',          'Turbo 1.4',          170,  8.5, 190, 'FWD', NULL, 4),
    ('Zephyr',   'Aeolus',        'Turbo 2.0',          340,  4.7, 255, 'RWD', NULL, 2),
    ('Zephyr',   'Boreas',        'Twin-turbo V6',      540,  3.8, 290, 'AWD', NULL, 4),
    ('Zephyr',   'Nimbus',        'Turbo V6',           390,  5.2, 245, 'AWD', NULL, 5),
    ('Zephyr',   'Cirrus',        'Turbo 2.0',          270,  6.0, 235, 'FWD', NULL, 4),
    ('Zephyr',   'Helios',        'Dual electric motors',610,  2.9, 320, 'AWD', 540, 4),
    ('Zephyr',   'Iris',          'Turbo 1.5',          210,  7.3, 200, 'FWD', NULL, 4)
) AS s(brand, name, engine, power_ps, acceleration_s, top_speed_kph, drivetrain, range_km, seats)
WHERE m.brand = s.brand AND m.name = s.name;

-- Guard: every model must end up with a complete specification.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM vehicle_model
        WHERE engine IS NULL OR power_ps IS NULL OR acceleration_s IS NULL
           OR top_speed_kph IS NULL OR drivetrain IS NULL OR seats IS NULL
    ) THEN
        RAISE EXCEPTION 'V4: some vehicle models are missing tech spec data';
    END IF;
END $$;

ALTER TABLE vehicle_model
    ALTER COLUMN engine SET NOT NULL,
    ALTER COLUMN power_ps SET NOT NULL,
    ALTER COLUMN acceleration_s SET NOT NULL,
    ALTER COLUMN top_speed_kph SET NOT NULL,
    ALTER COLUMN drivetrain SET NOT NULL,
    ALTER COLUMN seats SET NOT NULL;
