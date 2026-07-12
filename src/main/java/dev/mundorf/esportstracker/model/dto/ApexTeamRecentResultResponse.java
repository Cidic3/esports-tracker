package dev.mundorf.esportstracker.model.dto;

import java.time.Instant;
import java.util.UUID;

/** A team's placement in one recent ALGS match day - the "recent results" list on a team's page. */
public record ApexTeamRecentResultResponse(
        UUID matchDayId,
        String matchDayName,
        Instant startsAt,
        String tournamentName,
        int rank,
        int totalPoints
) {
}
