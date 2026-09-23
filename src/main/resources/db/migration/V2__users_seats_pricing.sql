-- V2: Users, show seats, pricing, discounts

CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    full_name    VARCHAR(200) NOT NULL,
    phone        VARCHAR(20),
    role         VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','CUSTOMER')),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE show_seats (
    id               BIGSERIAL PRIMARY KEY,
    show_id          BIGINT      NOT NULL REFERENCES shows(id),
    seat_id          BIGINT      NOT NULL REFERENCES seats(id),
    status           VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
                     CHECK (status IN ('AVAILABLE','HELD','BOOKED')),
    hold_expires_at  TIMESTAMP,
    held_by_user_id  BIGINT REFERENCES users(id),
    version          BIGINT NOT NULL DEFAULT 0,
    UNIQUE (show_id, seat_id)
);

CREATE INDEX idx_show_seats_show   ON show_seats(show_id);
CREATE INDEX idx_show_seats_status ON show_seats(show_id, status);
CREATE INDEX idx_show_seats_expiry ON show_seats(hold_expires_at) WHERE status = 'HELD';

CREATE TABLE pricing_tiers (
    id                        BIGSERIAL PRIMARY KEY,
    show_id                   BIGINT      NOT NULL REFERENCES shows(id),
    seat_type                 VARCHAR(20) NOT NULL CHECK (seat_type IN ('REGULAR','PREMIUM')),
    base_price                NUMERIC(10,2) NOT NULL,
    weekend_multiplier        NUMERIC(4,2) NOT NULL DEFAULT 1.0,
    weekend_pricing_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (show_id, seat_type)
);

CREATE TABLE discount_codes (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('FLAT','PERCENT')),
    value         NUMERIC(10,2) NOT NULL,
    max_uses      INT,
    used_count    INT NOT NULL DEFAULT 0,
    show_id       BIGINT REFERENCES shows(id),   -- NULL = applies to any show
    valid_from    TIMESTAMP NOT NULL,
    valid_to      TIMESTAMP NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
