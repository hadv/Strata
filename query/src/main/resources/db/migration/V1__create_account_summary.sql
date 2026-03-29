-- Read model: materialized view of account state
-- Projected from domain events consumed via Kafka (CDC)

CREATE TABLE IF NOT EXISTS account_summaries (
    account_id    VARCHAR(36)    PRIMARY KEY,
    account_name  VARCHAR(255)   NOT NULL,
    balance       DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    last_updated  TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    event_version BIGINT         NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for listing accounts sorted by name
CREATE INDEX idx_account_name ON account_summaries (account_name);
