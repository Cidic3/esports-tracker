package dev.mundorf.esportstracker.model.dto;

import dev.mundorf.esportstracker.model.entity.TournamentTier;

import java.util.UUID;

/** tier is derived (TournamentTier.forLeague), not stored on League — see LeagueMapper. */
public record LeagueResponse(UUID id, String name, String slug, String region, TournamentTier tier) {
}
