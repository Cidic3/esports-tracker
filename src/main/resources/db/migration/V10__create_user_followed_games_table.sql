CREATE TABLE user_followed_games (
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    game_id  UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, game_id)
);

CREATE INDEX idx_user_followed_games_game_id ON user_followed_games(game_id);
