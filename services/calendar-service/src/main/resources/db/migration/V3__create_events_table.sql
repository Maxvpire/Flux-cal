CREATE TABLE events (
    id VARCHAR(255) PRIMARY KEY,
    calendar_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    color_hex VARCHAR(255) NOT NULL,
    location_id VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    all_day BOOLEAN DEFAULT FALSE,
    sync_status VARCHAR(50) DEFAULT 'SYNCED',
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (calendar_id) REFERENCES calendars(id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL
);
