CREATE TABLE user_followed_teams (
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id  UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, team_id)
);

CREATE INDEX idx_user_followed_teams_team_id ON user_followed_teams(team_id);
