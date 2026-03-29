-- Event Store table for Event Sourcing
-- This is the single source of truth for all domain events

CREATE TABLE IF NOT EXISTS events (
    event_id       UUID         PRIMARY KEY,
    aggregate_id   UUID         NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    event_data     JSONB        NOT NULL,
    version        BIGINT       NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Optimistic concurrency: each aggregate can only have one event per version
    CONSTRAINT uq_aggregate_version UNIQUE (aggregate_id, version)
);

-- Index for fast aggregate reconstruction (load all events for an aggregate)
CREATE INDEX idx_events_aggregate_id ON events (aggregate_id, version ASC);

-- Index for Debezium CDC: efficient WAL-based replication
-- (PostgreSQL automatically tracks changes via WAL, but this helps queries)
CREATE INDEX idx_events_created_at ON events (created_at);

-- REPLICA IDENTITY FULL is required for Debezium to capture complete row data
ALTER TABLE events REPLICA IDENTITY FULL;
