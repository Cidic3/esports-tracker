package dev.mundorf.esportstracker.model.dto;

import java.util.List;

/** One team's row in an ALGS match day's results table. */
public record ApexTeamResultResponse(
        int rank,
        int totalPoints,
        TeamResponse team,
        List<ApexGameResultResponse> games
) {
}
