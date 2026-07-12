package dev.mundorf.esportstracker.model.dto;

import dev.mundorf.esportstracker.model.entity.EventStatus;

import java.time.Instant;
import java.util.UUID;

/** List-item shape for an ALGS match day - the Apex analog of MatchResponse, minus teamA/teamB. */
public record ApexMatchDayResponse(
        UUID id,
        String name,
        Instant startsAt,
        EventStatus status,
        UUID tournamentId,
        String tournamentName,
        String leagueSlug,
        String leagueName,
        String gameSlug
) {
}
