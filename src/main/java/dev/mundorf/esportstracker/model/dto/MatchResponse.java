package dev.mundorf.esportstracker.model.dto;

import dev.mundorf.esportstracker.model.entity.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        Instant scheduledAt,
        EventStatus status,
        Integer scoreA,
        Integer scoreB,
        String streamUrl,
        TeamResponse teamA,
        TeamResponse teamB,
        UUID tournamentId,
        String tournamentName,
        String gameSlug
) {
}
