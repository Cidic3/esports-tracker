package dev.mundorf.esportstracker.model.dto;

public record StandingResponse(
        String tournamentName, String groupName, int rank, int wins, int losses, TeamResponse team) {
}
