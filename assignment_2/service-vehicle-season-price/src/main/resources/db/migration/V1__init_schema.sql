CREATE TYPE season AS ENUM ('spring', 'summer', 'fall', 'winter');

CREATE TABLE vehicle_types (
    vehicle_type TEXT PRIMARY KEY
);

CREATE TABLE prices (
    vehicle_type   TEXT    NOT NULL REFERENCES vehicle_types (vehicle_type),
    season         season  NOT NULL,
    price_per_day  NUMERIC(6, 2) NOT NULL CHECK (price_per_day >= 0),
    PRIMARY KEY (vehicle_type, season)
);
