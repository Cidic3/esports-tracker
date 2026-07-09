-- League-level follows: the feed is driven by followed leagues + followed teams.
-- (Game-level follows remain, but only as a UI grouping — they no longer feed the feed.)
CREATE TABLE user_followed_leagues (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    league_id  UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, league_id)
);

CREATE INDEX idx_user_followed_leagues_league_id ON user_followed_leagues(league_id);
