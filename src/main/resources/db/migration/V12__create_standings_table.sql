CREATE TABLE standings (
    id             UUID PRIMARY KEY,
    tournament_id  UUID NOT NULL REFERENCES tournaments(id),
    team_id        UUID NOT NULL REFERENCES teams(id),
    group_name     VARCHAR(100) NOT NULL,
    rank_position  INTEGER NOT NULL,
    wins           INTEGER NOT NULL,
    losses         INTEGER NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_standings_tournament_team_group UNIQUE (tournament_id, team_id, group_name)
);

CREATE INDEX idx_standings_tournament_id ON standings(tournament_id);
