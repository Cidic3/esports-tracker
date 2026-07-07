CREATE TABLE leagues (
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(100) NOT NULL,
    region        VARCHAR(100),
    game_id       UUID NOT NULL REFERENCES games(id),
    external_id   VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    CONSTRAINT uk_leagues_game_slug UNIQUE (game_id, slug),
    CONSTRAINT uk_leagues_game_external_id UNIQUE (game_id, external_id)
);

CREATE INDEX idx_leagues_game_id ON leagues(game_id);
