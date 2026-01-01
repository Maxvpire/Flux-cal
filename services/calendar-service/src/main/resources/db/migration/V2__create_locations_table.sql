CREATE TABLE locations (
    id VARCHAR(255) PRIMARY KEY,
    place_name VARCHAR(255) NOT NULL,
    street_address TEXT,
    city VARCHAR(255),
    country VARCHAR(255),
    building_name VARCHAR(255),
    floor INTEGER,
    room VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    place_id VARCHAR(255)
);
