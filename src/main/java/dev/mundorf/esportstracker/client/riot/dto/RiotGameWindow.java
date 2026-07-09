package dev.mundorf.esportstracker.client.riot.dto;

import java.time.Instant;
import java.util.List;

/**
 * Flattened livestats "window" response: who is playing what (static per game)
 * plus team-level objective totals from the most recent frame at the requested time.
 * gameState comes from that frame: "in_game", "paused", or "finished".
 */
public record RiotGameWindow(
        Instant firstFrameTimestamp,
        String gameState,
        RiotGameTeamFrame blueTeam,
        RiotGameTeamFrame redTeam) {

    public record RiotGameTeamFrame(
            List<RiotParticipantMeta> participants,
            int totalGold,
            int towers,
            int barons,
            int totalKills,
            List<String> dragons) {
    }

    /** championId is a champion name string (e.g. "DrMundo"), resolvable via Data Dragon. */
    public record RiotParticipantMeta(int participantId, String summonerName, String championId, String role) {
    }
}
