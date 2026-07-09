package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.MatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MatchService {

    // Never a real UUID (Riot/Valve external data + JPA @GeneratedValue(UUID) both produce random
    // v4 UUIDs), used to keep an "IN" clause non-empty when a user follows nothing of that kind.
    private static final Set<UUID> NONE = Set.of(new UUID(0, 0));

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public Page<Match> search(String gameSlug, EventStatus status, UUID teamId,
                              Instant from, Instant to, Pageable pageable) {
        return matchRepository.search(gameSlug, status, teamId, from, to, pageable);
    }

    public Page<Match> findByTournament(UUID tournamentId, Pageable pageable) {
        return matchRepository.findByTournamentId(tournamentId, pageable);
    }

    public Match findById(UUID id) {
        return matchRepository.findWithAssociationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }

    /**
     * Upcoming matches for a user's followed leagues/teams, most imminent first. Game-level
     * follows are a UI grouping only and deliberately don't widen this query.
     */
    public Page<Match> findUpcomingForUser(User user, Pageable pageable) {
        Set<UUID> leagueIds = user.getFollowedLeagues().stream().map(League::getId).collect(Collectors.toSet());
        Set<UUID> teamIds = user.getFollowedTeams().stream().map(Team::getId).collect(Collectors.toSet());
        if (leagueIds.isEmpty() && teamIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return matchRepository.findUpcomingForFollowed(
                leagueIds.isEmpty() ? NONE : leagueIds,
                teamIds.isEmpty() ? NONE : teamIds,
                pageable);
    }

    /** Matches scheduled anytime during the current UTC calendar day, in chronological order. */
    public List<Match> findToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = today.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        return matchRepository.findByScheduledAtBetweenOrderByScheduledAtAsc(startOfDay, endOfDay);
    }
}
