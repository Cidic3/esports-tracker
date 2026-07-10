package dev.mundorf.esportstracker.model.dto;

import dev.mundorf.esportstracker.model.entity.PlayerRole;

import java.util.UUID;

/**
 * active: heuristic, not authoritative - Riot's roster data has no starter/substitute flag, so this
 * is inferred from whether the player appeared in any of the team's last few finished matches (see
 * TeamService.findActiveSummonerNames). A player benched for longer than that window would show as
 * inactive even if still on the roster.
 */
public record PlayerResponse(
        UUID id,
        String summonerName,
        String firstName,
        String lastName,
        String imageUrl,
        PlayerRole role,
        boolean active
) {
}
