package dev.mundorf.esportstracker.model.dto;

import java.util.List;

public record FeedResponse(
        List<MatchResponse> liveMatches,
        List<MatchResponse> upcomingMatches,
        List<TournamentResponse> runningTournaments) {
}
