-- Chesst initial schema (V1)
-- Users, openings, games, analyses

CREATE TABLE users (
    id                    BIGSERIAL PRIMARY KEY,
    username              VARCHAR(24)  NOT NULL UNIQUE,
    email                 VARCHAR(160) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    display_name          VARCHAR(80),
    bio                   TEXT,
    avatar_url            VARCHAR(500),
    rating                INTEGER      NOT NULL DEFAULT 1200,
    email_verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    verification_code     VARCHAR(12),
    verification_code_exp TIMESTAMPTZ,
    lichess_username      VARCHAR(48) UNIQUE,
    chesscom_username     VARCHAR(48) UNIQUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_email_lower    ON users (LOWER(email));
CREATE INDEX idx_users_username_lower ON users (LOWER(username));

CREATE TABLE openings (
    id           BIGSERIAL PRIMARY KEY,
    eco          VARCHAR(5)   NOT NULL,
    name         VARCHAR(200) NOT NULL,
    pgn          TEXT         NOT NULL,
    fen          TEXT,
    white_wins   INTEGER      NOT NULL DEFAULT 0,
    draws        INTEGER      NOT NULL DEFAULT 0,
    black_wins   INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_openings_eco  ON openings (eco);
CREATE INDEX idx_openings_name ON openings (name);

CREATE TABLE games (
    id             BIGSERIAL PRIMARY KEY,
    owner_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    white          VARCHAR(120) NOT NULL DEFAULT 'White',
    black          VARCHAR(120) NOT NULL DEFAULT 'Black',
    result         VARCHAR(16)  NOT NULL DEFAULT '*',
    event          VARCHAR(200),
    site           VARCHAR(200),
    date_played    VARCHAR(20),
    eco            VARCHAR(5),
    opening_name   VARCHAR(200),
    pgn            TEXT         NOT NULL,
    start_fen      TEXT,
    move_count     INTEGER      NOT NULL DEFAULT 0,
    source         VARCHAR(20)  NOT NULL DEFAULT 'manual',
    source_game_id VARCHAR(64),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_games_owner   ON games (owner_id);
CREATE INDEX idx_games_eco     ON games (eco);
CREATE INDEX idx_games_created ON games (created_at);

CREATE TABLE analyses (
    id          BIGSERIAL PRIMARY KEY,
    game_id     BIGINT  NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    user_id     BIGINT  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    depth       INTEGER NOT NULL,
    payload     TEXT    NOT NULL,
    accuracy_w  DOUBLE PRECISION,
    accuracy_b  DOUBLE PRECISION,
    blunders_w  INTEGER NOT NULL DEFAULT 0,
    blunders_b  INTEGER NOT NULL DEFAULT 0,
    mistakes_w  INTEGER NOT NULL DEFAULT 0,
    mistakes_b  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_analyses_game ON analyses (game_id);
CREATE INDEX idx_analyses_user ON analyses (user_id);
