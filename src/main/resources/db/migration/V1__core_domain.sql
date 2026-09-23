-- V1: Core domain tables

CREATE TABLE cities (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    state      VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE theaters (
    id         BIGSERIAL PRIMARY KEY,
    city_id    BIGINT NOT NULL REFERENCES cities(id),
    name       VARCHAR(200) NOT NULL,
    address    VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE screens (
    id         BIGSERIAL PRIMARY KEY,
    theater_id BIGINT NOT NULL REFERENCES theaters(id),
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE seats (
    id         BIGSERIAL PRIMARY KEY,
    screen_id  BIGINT NOT NULL REFERENCES screens(id),
    row_label  VARCHAR(5)  NOT NULL,
    seat_number INT        NOT NULL,
    seat_type  VARCHAR(20) NOT NULL CHECK (seat_type IN ('REGULAR','PREMIUM')),
    UNIQUE (screen_id, row_label, seat_number)
);

CREATE TABLE movies (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(300) NOT NULL,
    description   TEXT,
    duration_mins INT,
    genre         VARCHAR(100),
    language      VARCHAR(50),
    release_date  DATE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE shows (
    id         BIGSERIAL PRIMARY KEY,
    screen_id  BIGINT      NOT NULL REFERENCES screens(id),
    movie_id   BIGINT      NOT NULL REFERENCES movies(id),
    start_time TIMESTAMP   NOT NULL,
    end_time   TIMESTAMP   NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                CHECK (status IN ('SCHEDULED','CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shows_start_time ON shows(start_time);
CREATE INDEX idx_shows_screen     ON shows(screen_id);
CREATE INDEX idx_shows_movie      ON shows(movie_id);
