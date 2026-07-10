package dev.mundorf.esportstracker.model.dto;

import java.util.List;
import java.util.UUID;

public record TeamDetailResponse(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        String gameSlug,
        OrganizationResponse organization,
        List<PlayerResponse> roster,
        List<StandingResponse> standings,
        List<MatchResponse> liveMatches,
        List<MatchResponse> recentMatches,
        List<MatchResponse> upcomingMatches
) {
}
