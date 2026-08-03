-- Seed data transcribed from assignment_2/assignment/02VEHI~1.JSO (the assignment's provided
-- JSON pricing dataset).

INSERT INTO vehicle_types (vehicle_type) VALUES
    ('Compact'),
    ('Sedan'),
    ('SUV'),
    ('Convertible'),
    ('Truck');

INSERT INTO prices (vehicle_type, season, price_per_day) VALUES
    ('Compact',     'spring', 45),
    ('Compact',     'summer', 50),
    ('Compact',     'fall',   43),
    ('Compact',     'winter', 40),

    ('Sedan',       'spring', 55),
    ('Sedan',       'summer', 60),
    ('Sedan',       'fall',   53),
    ('Sedan',       'winter', 50),

    ('SUV',         'spring', 65),
    ('SUV',         'summer', 70),
    ('SUV',         'fall',   63),
    ('SUV',         'winter', 60),

    ('Convertible', 'spring', 75),
    ('Convertible', 'summer', 80),
    ('Convertible', 'fall',   73),
    ('Convertible', 'winter', 70),

    ('Truck',       'spring', 85),
    ('Truck',       'summer', 90),
    ('Truck',       'fall',   83),
    ('Truck',       'winter', 80);
