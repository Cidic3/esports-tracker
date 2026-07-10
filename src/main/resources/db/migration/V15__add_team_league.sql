ALTER TABLE teams ADD COLUMN league_id UUID REFERENCES leagues(id);
CREATE INDEX idx_teams_league_id ON teams(league_id);
