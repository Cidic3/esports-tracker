package dev.mundorf.esportstracker.client.riot.dto;

import java.util.List;

/** From Riot's getTeams endpoint - the only source of roster/player data available (no coach field exists in it). */
public record RiotTeam(
        String id,
        String slug,
        String name,
        String image,
        String status,
        RiotHomeLeague homeLeague,
        List<RiotPlayer> players
) {
}
