CREATE TABLE conferences (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    google_conference_id VARCHAR(255),
    meet_link TEXT,
    meeting_code VARCHAR(255),
    phone_number VARCHAR(255),
    pin VARCHAR(255),
    conference_link TEXT,
    conference_password VARCHAR(255),
    platform_name VARCHAR(255),
    sync_status VARCHAR(50),
    last_synced TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

ALTER TABLE events ADD COLUMN conference_id VARCHAR(255) UNIQUE;
ALTER TABLE events ADD CONSTRAINT fk_events_conference FOREIGN KEY (conference_id) REFERENCES conferences(id) ON DELETE SET NULL;
