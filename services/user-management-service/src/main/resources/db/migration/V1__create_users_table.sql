CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    birthday DATE NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
