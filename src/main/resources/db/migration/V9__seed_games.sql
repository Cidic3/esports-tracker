-- Seed the two supported games with fixed UUIDs (deterministic across dev/test/prod, unlike
-- gen_random_uuid()). slug is the stable lookup key the sync jobs use to attach synced data.
INSERT INTO games (id, name, slug, icon_url, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'League of Legends', 'league-of-legends', NULL, now(), now()),
    ('22222222-2222-2222-2222-222222222222', 'Dota 2', 'dota-2', NULL, now(), now());
