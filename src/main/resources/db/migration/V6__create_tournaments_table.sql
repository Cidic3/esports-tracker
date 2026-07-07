CREATE TABLE tournaments (
    id            UUID PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    slug          VARCHAR(150) NOT NULL,
    league_id     UUID REFERENCES leagues(id),
    game_id       UUID NOT NULL REFERENCES games(id),
    start_date    DATE NOT NULL,
    end_date      DATE NOT NULL,
    tier          VARCHAR(20) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    prize_pool    NUMERIC(12, 2),
    external_id   VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    CONSTRAINT uk_tournaments_game_slug UNIQUE (game_id, slug),
    CONSTRAINT uk_tournaments_game_external_id UNIQUE (game_id, external_id)
);

CREATE INDEX idx_tournaments_game_id ON tournaments(game_id);
CREATE INDEX idx_tournaments_league_id ON tournaments(league_id);
CREATE INDEX idx_tournaments_status ON tournaments(status);
