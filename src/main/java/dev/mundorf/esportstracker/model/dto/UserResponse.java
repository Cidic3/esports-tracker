package dev.mundorf.esportstracker.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** version: pass back unchanged on the next follow-update PUT - see StaleUpdateException. */
public record UserResponse(
        UUID id,
        String username,
        String email,
        Instant createdAt,
        List<GameResponse> followedGames,
        List<LeagueResponse> followedLeagues,
        List<TeamResponse> followedTeams,
        long version
) {
}
