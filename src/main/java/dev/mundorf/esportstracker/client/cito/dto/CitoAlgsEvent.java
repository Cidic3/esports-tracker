package dev.mundorf.esportstracker.client.cito.dto;

import java.time.Instant;
import java.util.List;

/**
 * One ALGS "match day" from Cito's /apex/algs/events endpoint (e.g. "Group A vs B" of Split 1
 * Pro League EMEA on a given date). statsData is only present once the day has been played -
 * pending events carry the schedule fields alone. Cito returns far more inside statsData (legend
 * bans, pick rates, player performance); only the team scores are mapped here - the rest is
 * display-only depth that can be added later without resyncing.
 */
public record CitoAlgsEvent(
        String id,
        String name,
        String eventName,
        String region,
        Instant startsAt,
        String status,
        String yearSlug,
        String eventSlug,
        CitoStatsData statsData
) {

    public record CitoStatsData(List<CitoTeamScore> scores) {
    }

    public record CitoTeamScore(
            int rank,
            String teamName,
            int totalScore,
            List<CitoGameScore> games
    ) {
    }

    public record CitoGameScore(int gameNumber, int placement, int kills, int points) {
    }
}
