CREATE TABLE notification_tasks (
    id             BIGSERIAL PRIMARY KEY,
    trip_id        BIGINT       NOT NULL,
    recipient_type VARCHAR(10)  NOT NULL,
    recipient_id   BIGINT       NOT NULL,
    message        VARCHAR(2000) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts       SMALLINT     NOT NULL DEFAULT 0,
    last_error     VARCHAR(1000),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_notification_status CHECK (status IN (
        'PENDING', 'IN_PROGRESS', 'SENT', 'FAILED', 'DEAD'
    )),
    CONSTRAINT chk_recipient_type CHECK (recipient_type IN ('PASSENGER', 'DRIVER'))
);

CREATE INDEX idx_notification_tasks_status_id ON notification_tasks(status, id);
CREATE INDEX idx_notification_tasks_trip_id ON notification_tasks(trip_id);
CREATE INDEX idx_notification_tasks_recipient_id ON notification_tasks(recipient_id);
