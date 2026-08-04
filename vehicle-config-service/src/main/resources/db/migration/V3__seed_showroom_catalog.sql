-- V3: expand the showroom catalogue with 50 additional models.
--
-- The initial catalogue (V2) contains the two "Aster" models used as stable
-- test fixtures. This migration adds 50 more models across 8 fictional
-- brands, each with a "Standard" trim plus one rotating edition trim, and a
-- rotating set of 4-6 options from a shared template pool.
--
-- Notes:
--   * Model IDs are generated at runtime and reused within each loop
--     iteration so trims/options reference the exact model row.
--   * Deliberately NOT idempotent: Flyway runs this versioned migration
--     exactly once. No ON CONFLICT clauses.
--   * Every label and description below avoids the substrings "vale",
--     "terra", "apex" and "panoramic" (case-insensitive) so the outside-in
--     browser test selectors remain unambiguous.
--   * All prices stay well below the 2,147,483,647 ceiling enforced by the
--     schema CHECK constraints and GraphQL Int.

DO $$
DECLARE
    item      record;
    model_id  uuid;
    ordinal   integer := 0;
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            ('Veloce',    'Nera GT',      2026,  5890000, 'The Nera GT pairs a lightweight shell with a sonorous twin-turbo power unit.'),
            ('Veloce',    'Sera Coupe',   2026,  4980000, 'A sculpted two-door coupe honed for mountain passes and evening drives.'),
            ('Veloce',    'Tigra Roadster', 2025, 4250000, 'Open-top thrills with a fabric roof that folds in twelve seconds.'),
            ('Veloce',    'Mira Berlina', 2026,  5240000, 'A fast four-door grand tourer with generous space and sharp manners.'),
            ('Veloce',    'Zero EV',      2026,  5790000, 'Fully electric flagship with 480 miles of range and instant torque.'),
            ('Veloce',    'Nova S',       2025,  3690000, 'The compact entry to the marque, keen handling and clean styling.'),
            ('Veloce',    'Cento',        2026,  6980000, 'A limited-run flagship celebrating a century of speed.'),
            ('Orbis',     'Luxus',        2026,  6450000, 'A quiet, spacious luxury saloon with every convenience as standard.'),
            ('Orbis',     'Vario',        2026,  4720000, 'A versatile five-door with modular seating and a practical boot.'),
            ('Orbis',     'Kammer',       2025,  3980000, 'A taut hatchback that rewards the keen driver in the city.'),
            ('Orbis',     'Sturm',        2026,  6890000, 'Muscular saloon with all-wheel drive and a thunderous straight-six.'),
            ('Orbis',     'Freya',        2025,  4450000, 'Elegant estate with a calm cabin and composed long-distance gait.'),
            ('Orbis',     'Orion',        2026,  5150000, 'Executive tourer blending restraint with genuine pace.'),
            ('Orbis',     'Grenz',        2025,  4980000, 'A disciplined executive saloon with crisp handling.'),
            ('Solstice',  'Ember',        2026,  4150000, 'A lithe sports car with a hand-finished interior and a free-revving engine.'),
            ('Solstice',  'Crest',        2026,  5340000, 'A grand tourer built for cross-country journeys in total comfort.'),
            ('Solstice',  'Marlin',       2025,  3620000, 'Focused two-seater with a balanced chassis and tactile steering.'),
            ('Solstice',  'Pinnacle',     2026,  7450000, 'The flagship coupe, coachbuilt in small numbers to order.'),
            ('Solstice',  'Voyager',      2025,  4680000, 'A refined five-door tourer with room for the whole family.'),
            ('Solstice',  'Tern',         2026,  3480000, 'Light, precise and honest; a back-to-basics driver''s car.'),
            ('Hajime',    'Kaze',         2026,  3750000, 'A nimble compact with a high-revving hybrid drivetrain.'),
            ('Hajime',    'Yuki',         2025,  4350000, 'Winter-proof saloon with all-wheel drive and exceptional build quality.'),
            ('Hajime',    'Sora',         2026,  5050000, 'A serene crossover with a hushed electric powertrain.'),
            ('Hajime',    'Hikari',       2025,  4050000, 'A bright, efficient hatchback with a futuristic cockpit.'),
            ('Hajime',    'Sakura',       2026,  3250000, 'A charming city car with surprising agility and low running costs.'),
            ('Hajime',    'Raiden',       2026,  5550000, 'Electric performance saloon with relentless acceleration.'),
            ('Auriga',    'Fjord',        2026,  4880000, 'A rugged crossover built for cold climates and rough roads.'),
            ('Auriga',    'Polar',        2025,  5320000, 'A hunkered-down flagship with immense traction and calm poise.'),
            ('Auriga',    'Boreal',       2026,  4250000, 'An estate with a minimalist cabin and a sturdy, honest character.'),
            ('Auriga',    'Nival',        2025,  3880000, 'A snow-ready compact that stays composed in the worst weather.'),
            ('Auriga',    'Drift',        2026,  4620000, 'A poised sports saloon with balanced weight and crisp responses.'),
            ('Auriga',    'Iskold',       2025,  3420000, 'An efficient city crossover with a warm, spacious interior.'),
            ('Cascadia',  'Rogue',        2026,  3980000, 'A no-nonsense utility SUV for work and weekend escapes.'),
            ('Cascadia',  'Canyon',       2025,  4120000, 'An off-road ready machine with locking diffs and hill descent.'),
            ('Cascadia',  'Mesa',         2026,  4680000, 'A bold three-row SUV with generous towing capacity.'),
            ('Cascadia',  'Butte',        2025,  3680000, 'A rugged mid-size utility with a durable, wash-down cabin.'),
            ('Cascadia',  'Summit',       2026,  5890000, 'The luxury SUV flagship with a commanding driving position.'),
            ('Cascadia',  'Aurora',       2026,  6150000, 'A wide-body grand tourer with American muscle and modern tech.'),
            ('Mirage',    'Lumine',       2026,  4350000, 'A softly styled saloon with a hushed ride and refined manners.'),
            ('Mirage',    'Aria',         2025,  3580000, 'A graceful hatchback with an airy, light-filled cabin.'),
            ('Mirage',    'Charme',       2026,  3980000, 'A chic city car that turns heads and sips fuel.'),
            ('Mirage',    'Coeur',        2025,  4780000, 'A passionate grand tourer with a sonorous six-cylinder.'),
            ('Mirage',    'Vague',        2026,  4150000, 'A fluid crossover with flowing lines and a calm drive.'),
            ('Mirage',    'Ciel',         2026,  3320000, 'A compact convertible with an easy, carefree character.'),
            ('Zephyr',    'Aeolus',       2026,  5420000, 'A light, eager sports car with a wind-tunnel-tuned body.'),
            ('Zephyr',    'Boreas',       2025,  6240000, 'A powerful all-weather grand tourer for fast winter travel.'),
            ('Zephyr',    'Nimbus',       2026,  4920000, 'A cloud-smooth SUV with an exceptionally quiet cabin.'),
            ('Zephyr',    'Cirrus',       2025,  4180000, 'A sleek fastback with relaxed cruising manners.'),
            ('Zephyr',    'Helios',       2026,  5780000, 'A solar-charged hybrid saloon with effortless pace.'),
            ('Zephyr',    'Iris',         2026,  3890000, 'A colourful city crossover with vibrant trim options.')
        ) AS catalogue(brand, name, model_year, base_price_cents, description)
    LOOP
        ordinal := ordinal + 1;
        model_id := gen_random_uuid();

        INSERT INTO vehicle_model (id, brand, name, model_year, base_price_cents, description)
        VALUES (model_id, item.brand, item.name, item.model_year,
                item.base_price_cents, item.description);

        -- Every model gets a "Standard" edition plus one rotating edition trim.
        INSERT INTO trim (id, model_id, name, price_delta_cents)
        VALUES
            (gen_random_uuid(), model_id, 'Standard', 0),
            (gen_random_uuid(), model_id,
             (ARRAY['Sport', 'Grand', 'Summit', 'Rally', 'Luxe'])[1 + ((ordinal - 1) % 5)],
             180000 + (((ordinal - 1) % 5) * 45000));

        -- Rotating set of 4-6 options per model from the shared template pool.
        INSERT INTO vehicle_option (id, model_id, name, category, price_cents)
        SELECT gen_random_uuid(), model_id, name, category, price_cents
        FROM (VALUES
            (0, 'Launch control',       'PERFORMANCE', 185000),
            (1, 'Heated steering wheel','COMFORT',      65000),
            (2, 'Driver display',       'TECHNOLOGY',  120000),
            (3, 'Paint protection',     'EXTERIOR',     95000),
            (4, 'Adaptive dampers',     'PERFORMANCE', 210000),
            (5, 'Premium audio',        'TECHNOLOGY',  155000)
        ) AS templates(slot, name, category, price_cents)
        WHERE (slot + ordinal) % 6 < 4 + ((ordinal - 1) % 3);
    END LOOP;
END $$;
