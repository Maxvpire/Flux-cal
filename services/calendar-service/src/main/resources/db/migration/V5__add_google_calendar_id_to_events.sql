ALTER TABLE events
ADD COLUMN google_calendar_id VARCHAR(255);

CREATE INDEX idx_events_google_calendar_id ON events(google_calendar_id);
