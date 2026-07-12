package dev.mundorf.esportstracker.model.dto;

import dev.mundorf.esportstracker.model.entity.EventStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full match day view: the day itself plus the standings-style results table (one row per team,
 * ordered by rank, each with its per-game placement/kills/points breakdown). results is empty
 * for UPCOMING days - a pending BR match day has no team list yet.
 */
public record ApexMatchDayDetailResponse(
        UUID id,
        String name,
        Instant startsAt,
        EventStatus status,
        UUID tournamentId,
        String tournamentName,
        String leagueSlug,
        String leagueName,
        String gameSlug,
        List<ApexTeamResultResponse> results
) {
}
