package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.MatchResponse;
import dev.mundorf.esportstracker.model.dto.OrganizationResponse;
import dev.mundorf.esportstracker.model.dto.PlayerResponse;
import dev.mundorf.esportstracker.model.dto.StandingResponse;
import dev.mundorf.esportstracker.model.dto.TeamDetailResponse;
import dev.mundorf.esportstracker.model.dto.TeamResponse;
import dev.mundorf.esportstracker.model.dto.TeamSummaryResponse;
import dev.mundorf.esportstracker.model.entity.Organization;
import dev.mundorf.esportstracker.model.entity.Team;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeamMapper {

    public TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getSlug(), team.getLogoUrl());
    }

    public TeamSummaryResponse toSummaryResponse(Team team) {
        return new TeamSummaryResponse(
                team.getId(), team.getName(), team.getSlug(), team.getLogoUrl(), team.getGame().getSlug());
    }

    /**
     * Accepts already-mapped roster/standings/match lists rather than injecting PlayerMapper/
     * StandingMapper/MatchMapper here - StandingMapper and MatchMapper already depend on TeamMapper,
     * so the reverse dependency would be circular. The controller composes the pieces instead.
     */
    public TeamDetailResponse toDetailResponse(Team team, List<PlayerResponse> roster,
                                               List<StandingResponse> standings,
                                               List<MatchResponse> liveMatches,
                                               List<MatchResponse> recentMatches,
                                               List<MatchResponse> upcomingMatches) {
        return new TeamDetailResponse(
                team.getId(),
                team.getName(),
                team.getSlug(),
                team.getLogoUrl(),
                team.getGame().getSlug(),
                toOrganizationResponse(team.getOrganization()),
                roster,
                standings,
                liveMatches,
                recentMatches,
                upcomingMatches);
    }

    private OrganizationResponse toOrganizationResponse(Organization organization) {
        if (organization == null) {
            return null;
        }
        return new OrganizationResponse(
                organization.getId(), organization.getName(), organization.getSlug(), organization.getLogoUrl());
    }
}
