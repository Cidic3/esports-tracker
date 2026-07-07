CREATE TABLE matches (
    id             UUID PRIMARY KEY,
    tournament_id  UUID NOT NULL REFERENCES tournaments(id),
    game_id        UUID NOT NULL REFERENCES games(id),
    team_a_id      UUID NOT NULL REFERENCES teams(id),
    team_b_id      UUID NOT NULL REFERENCES teams(id),
    scheduled_at   TIMESTAMP NOT NULL,
    status         VARCHAR(20) NOT NULL,
    score_a        INTEGER,
    score_b        INTEGER,
    stream_url     VARCHAR(500),
    external_id    VARCHAR(100) NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_matches_game_external_id UNIQUE (game_id, external_id)
);

CREATE INDEX idx_matches_scheduled_at ON matches(scheduled_at);
CREATE INDEX idx_matches_status ON matches(status);
CREATE INDEX idx_matches_tournament_id ON matches(tournament_id);
CREATE INDEX idx_matches_team_a_id ON matches(team_a_id);
CREATE INDEX idx_matches_team_b_id ON matches(team_b_id);
