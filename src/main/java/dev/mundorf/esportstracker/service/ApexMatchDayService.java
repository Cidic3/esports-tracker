package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.ApexMatchDay;
import dev.mundorf.esportstracker.model.entity.ApexTeamResult;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.ApexMatchDayRepository;
import dev.mundorf.esportstracker.repository.ApexTeamResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApexMatchDayService {

    private static final String APEX_GAME_SLUG = "apex-legends";
    private static final int RECENT_RESULTS_LIMIT = 5;

    private final ApexMatchDayRepository matchDayRepository;
    private final ApexTeamResultRepository teamResultRepository;

    public ApexMatchDayService(ApexMatchDayRepository matchDayRepository,
                               ApexTeamResultRepository teamResultRepository) {
        this.matchDayRepository = matchDayRepository;
        this.teamResultRepository = teamResultRepository;
    }

    public Page<ApexMatchDay> search(String leagueSlug, EventStatus status, Pageable pageable) {
        return matchDayRepository.search(leagueSlug, status, pageable);
    }

    public ApexMatchDay findById(UUID id) {
        return matchDayRepository.findWithAssociationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apex match day not found: " + id));
    }

    public List<ApexTeamResult> findResults(UUID matchDayId) {
        return teamResultRepository.findByMatchDayIdOrderByRankAsc(matchDayId);
    }

    /**
     * Upcoming ALGS match days for a user's follows. League follows work exactly like LoL's. Team
     * follows need a twist: a pending BR match day has no team list yet (teams only appear once
     * results land), so a followed Apex team is resolved to its home league (its ALGS region,
     * stamped by CitoSyncService) - following a team implies caring about its region's schedule.
     * LoL team follows deliberately don't get this treatment; league-widening is only justified
     * here because Apex has no per-team schedule to match against.
     */
    public Page<ApexMatchDay> findUpcomingForUser(User user, Pageable pageable) {
        Set<UUID> leagueIds = user.getFollowedLeagues().stream()
                .map(League::getId)
                .collect(Collectors.toCollection(HashSet::new));
        user.getFollowedTeams().stream()
                .filter(team -> APEX_GAME_SLUG.equals(team.getGame().getSlug()))
                .map(Team::getLeague)
                .filter(league -> league != null)
                .map(League::getId)
                .forEach(leagueIds::add);
        if (leagueIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return matchDayRepository.findUpcomingForLeagues(leagueIds, pageable);
    }

    /**
     * A team's most recent match day placements, newest first - for the team detail page.
     * The ordering lives in the repository's JPQL (ORDER BY startsAt DESC), so the pageable
     * here is deliberately unsorted - passing a Sort too would double up the ORDER BY clause.
     */
    public List<ApexTeamResult> findRecentResultsForTeam(UUID teamId) {
        return teamResultRepository.findRecentByTeamId(teamId, PageRequest.of(0, RECENT_RESULTS_LIMIT))
                .getContent();
    }
}
