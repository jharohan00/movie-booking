-- V3: Bookings, payments, refund policies, notifications

CREATE TABLE bookings (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users(id),
    show_id          BIGINT      NOT NULL REFERENCES shows(id),
    discount_code_id BIGINT      REFERENCES discount_codes(id),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','CONFIRMED','CANCELLED')),
    total_amount     NUMERIC(10,2),
    discount_amount  NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_show ON bookings(show_id);

CREATE TABLE booking_items (
    id           BIGSERIAL PRIMARY KEY,
    booking_id   BIGINT NOT NULL REFERENCES bookings(id),
    show_seat_id BIGINT NOT NULL REFERENCES show_seats(id),
    price_paid   NUMERIC(10,2) NOT NULL,
    UNIQUE (booking_id, show_seat_id)
);

CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT      NOT NULL REFERENCES bookings(id),
    amount          NUMERIC(10,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
    transaction_ref VARCHAR(100),
    refund_amount   NUMERIC(10,2),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_booking ON payments(booking_id);

CREATE TABLE refund_policies (
    id                  BIGSERIAL PRIMARY KEY,
    theater_id          BIGINT REFERENCES theaters(id),  -- NULL = global default
    show_id             BIGINT REFERENCES shows(id),     -- show-level override
    hours_before_show   INT NOT NULL,
    refund_percent      NUMERIC(5,2) NOT NULL,
    UNIQUE (theater_id, hours_before_show),
    UNIQUE (show_id, hours_before_show)
);

CREATE TABLE notifications (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id),
    booking_id        BIGINT REFERENCES bookings(id),
    notification_type VARCHAR(30) NOT NULL
                      CHECK (notification_type IN ('CONFIRMATION','CANCELLATION','REMINDER')),
    message           TEXT,
    sent_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user    ON notifications(user_id);
CREATE INDEX idx_notifications_booking ON notifications(booking_id);
