CREATE TABLE IF NOT EXISTS jwt_db.public.processed_kafka_events (
    event_id     VARCHAR(200) NOT NULL,
    topic        VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_processed_kafka_events PRIMARY KEY (event_id)
);
