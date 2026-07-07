CREATE TABLE teams (
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(100) NOT NULL,
    logo_url      VARCHAR(500),
    game_id       UUID NOT NULL REFERENCES games(id),
    external_id   VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    CONSTRAINT uk_teams_game_slug UNIQUE (game_id, slug),
    CONSTRAINT uk_teams_game_external_id UNIQUE (game_id, external_id)
);

CREATE INDEX idx_teams_game_id ON teams(game_id);
