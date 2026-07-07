CREATE TABLE games (
    id             UUID PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    slug           VARCHAR(100) NOT NULL,
    icon_url       VARCHAR(500),
    pandascore_id  BIGINT,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_games_slug UNIQUE (slug),
    CONSTRAINT uk_games_pandascore_id UNIQUE (pandascore_id)
);
