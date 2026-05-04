CREATE TABLE trips (
    id            BIGSERIAL PRIMARY KEY,
    passenger_id  BIGINT       NOT NULL,
    driver_id     BIGINT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'SEARCHING',
    origin        TEXT         NOT NULL,
    destination   TEXT         NOT NULL,
    distance_km   NUMERIC(10,2) NOT NULL,
    price         NUMERIC(10,2),
    rating        SMALLINT     CHECK (rating BETWEEN 1 AND 5),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_trip_status CHECK (status IN (
        'SEARCHING', 'ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
    ))
);

CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_trips_passenger_id ON trips(passenger_id);
CREATE INDEX idx_trips_driver_id ON trips(driver_id);
CREATE INDEX idx_trips_created_at ON trips(created_at);
