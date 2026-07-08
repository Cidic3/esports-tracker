package dev.mundorf.esportstracker.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        Instant createdAt,
        List<GameResponse> followedGames,
        List<TeamResponse> followedTeams
) {
}
