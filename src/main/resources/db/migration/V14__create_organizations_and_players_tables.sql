CREATE TABLE organizations (
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL,
    logo_url   VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_organizations_slug UNIQUE (slug)
);

ALTER TABLE teams ADD COLUMN organization_id UUID REFERENCES organizations(id);
CREATE INDEX idx_teams_organization_id ON teams(organization_id);

CREATE TABLE players (
    id             UUID PRIMARY KEY,
    team_id        UUID NOT NULL REFERENCES teams(id),
    summoner_name  VARCHAR(100) NOT NULL,
    first_name     VARCHAR(100),
    last_name      VARCHAR(100),
    image_url      VARCHAR(500),
    role           VARCHAR(20) NOT NULL,
    external_id    VARCHAR(100) NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_players_team_external UNIQUE (team_id, external_id)
);

CREATE INDEX idx_players_team_id ON players(team_id);
