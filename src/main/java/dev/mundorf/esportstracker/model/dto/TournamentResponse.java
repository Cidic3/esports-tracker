package dev.mundorf.esportstracker.model.dto;

import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.TournamentTier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TournamentResponse(
        UUID id,
        String name,
        String slug,
        TournamentTier tier,
        EventStatus status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal prizePool,
        String gameSlug,
        LeagueResponse league
) {
}
