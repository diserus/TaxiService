ALTER TABLE trips ADD COLUMN idempotency_key VARCHAR(64);

CREATE UNIQUE INDEX uq_trips_passenger_idempotency_key
    ON trips(passenger_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
