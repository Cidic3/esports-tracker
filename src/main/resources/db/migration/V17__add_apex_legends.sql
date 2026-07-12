-- Apex Legends support (data source: Cito API aggregating official ALGS results - see CLAUDE.md).
-- Same fixed-UUID seeding convention as V9.
INSERT INTO games (id, name, slug, icon_url, created_at, updated_at) VALUES
    ('33333333-3333-3333-3333-333333333333', 'Apex Legends', 'apex-legends', NULL, now(), now());

-- A Battle Royale "match day" (e.g. "Group A vs B" on a given date): the Apex analog of a Match,
-- but with ~20 teams competing simultaneously instead of teamA/teamB - which is why these are
-- separate tables rather than nullable-teamB rows in matches.
CREATE TABLE apex_match_days (
    id           UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES tournaments(id),
    game_id      UUID NOT NULL REFERENCES games(id),
    name         VARCHAR(200) NOT NULL,
    starts_at    TIMESTAMP NOT NULL,
    status       VARCHAR(20) NOT NULL,
    external_id  VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    CONSTRAINT uk_apex_match_days_game_external UNIQUE (game_id, external_id)
);

CREATE INDEX idx_apex_match_days_tournament_id ON apex_match_days(tournament_id);
CREATE INDEX idx_apex_match_days_starts_at ON apex_match_days(starts_at);
CREATE INDEX idx_apex_match_days_status ON apex_match_days(status);

-- One team's cumulative result across all games of a match day (rank + total points).
CREATE TABLE apex_team_results (
    id            UUID PRIMARY KEY,
    match_day_id  UUID NOT NULL REFERENCES apex_match_days(id),
    team_id       UUID NOT NULL REFERENCES teams(id),
    rank_position INT NOT NULL,
    total_points  INT NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    CONSTRAINT uk_apex_team_results_day_team UNIQUE (match_day_id, team_id)
);

CREATE INDEX idx_apex_team_results_match_day_id ON apex_team_results(match_day_id);
CREATE INDEX idx_apex_team_results_team_id ON apex_team_results(team_id);

-- One team's result in a single game of a match day (placement + kills + points).
CREATE TABLE apex_game_results (
    id             UUID PRIMARY KEY,
    team_result_id UUID NOT NULL REFERENCES apex_team_results(id),
    game_number    INT NOT NULL,
    placement      INT NOT NULL,
    kills          INT NOT NULL,
    points         INT NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_apex_game_results_result_game UNIQUE (team_result_id, game_number)
);

CREATE INDEX idx_apex_game_results_team_result_id ON apex_game_results(team_result_id);
