package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ExternalApiException;
import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse.GameDetails;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse.TeamGameDetails;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Player;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.repository.MatchRepository;
import dev.mundorf.esportstracker.repository.PlayerRepository;
import dev.mundorf.esportstracker.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    // Detail page shows a short at-a-glance list, not a full match history browser - the existing
    // paginated /api/matches?team= endpoint covers that if a user wants the full list.
    private static final int RECENT_MATCHES_LIMIT = 5;
    private static final int UPCOMING_MATCHES_LIMIT = 5;

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final TeamSyncTrigger teamSyncTrigger;
    private final MatchDetailsService matchDetailsService;
    private final CacheManager cacheManager;

    public TeamService(TeamRepository teamRepository, PlayerRepository playerRepository,
                       MatchRepository matchRepository, TeamSyncTrigger teamSyncTrigger,
                       MatchDetailsService matchDetailsService, CacheManager cacheManager) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.teamSyncTrigger = teamSyncTrigger;
        this.matchDetailsService = matchDetailsService;
        this.cacheManager = cacheManager;
    }

    public Page<Team> search(String gameSlug, String search, Pageable pageable) {
        return teamRepository.search(gameSlug, search, pageable);
    }

    /**
     * Reads the team immediately from whatever's currently in the DB - never blocks on Riot. Fires
     * a background sync of the team's league (see TeamSyncTrigger) so a *later* visit is fresher,
     * throttled via RiotSyncService.syncLeagueOnDemand's cache so repeat visits are cheap.
     */
    public Team findById(UUID id) {
        Team team = teamRepository.findWithAssociationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
        if (team.getLeague() != null) {
            teamSyncTrigger.triggerLeagueSync(team.getLeague());
        }
        return team;
    }

    /** Roster grouped by lane role - PlayerRole's declaration order doubles as display order. */
    public List<Player> findRoster(UUID teamId) {
        return playerRepository.findByTeamId(teamId).stream()
                .sorted(Comparator.comparing(Player::getRole))
                .toList();
    }

    public List<Match> findRecentMatches(UUID teamId) {
        Pageable page = PageRequest.of(0, RECENT_MATCHES_LIMIT, Sort.by(Sort.Direction.DESC, "scheduledAt"));
        return matchRepository.search(null, EventStatus.FINISHED, teamId, null, null, page).getContent();
    }

    public List<Match> findUpcomingMatches(UUID teamId) {
        Pageable page = PageRequest.of(0, UPCOMING_MATCHES_LIMIT, Sort.by(Sort.Direction.ASC, "scheduledAt"));
        return matchRepository.search(null, EventStatus.UPCOMING, teamId, null, null, page).getContent();
    }

    /**
     * Matches currently in progress for this team. Deliberately separate from
     * findUpcomingMatches/findRecentMatches: a RUNNING match is in neither of those (no longer
     * UPCOMING, not FINISHED yet), so without this it briefly disappears from the team page the
     * moment it goes live.
     */
    public List<Match> findLiveMatches(UUID teamId) {
        Pageable page = PageRequest.of(0, UPCOMING_MATCHES_LIMIT, Sort.by(Sort.Direction.ASC, "scheduledAt"));
        return matchRepository.search(null, EventStatus.RUNNING, teamId, null, null, page).getContent();
    }

    /**
     * Heuristic "who's actually playing" set, since Riot's roster data has no starter/substitute
     * flag (confirmed by direct probing - see PlayerResponse javadoc): a player who appeared in any
     * of the team's recent finished matches counts as active.
     *
     * <p>Reads the matchDetails cache only - never calls Riot itself. A cache miss (first time this
     * match's details are needed) just means that match contributes nothing this time, rather than
     * blocking the page load on Riot's live-stats "walk" (which can take tens of seconds - see
     * MatchDetailsService). {@link #warmMatchDetails} fires the real fetches in the background so a
     * later visit/refetch sees a fuller (and eventually complete, once all 5 are cached) picture.
     */
    public Set<String> findActiveSummonerNames(UUID teamId, List<Match> recentMatches) {
        Cache cache = cacheManager.getCache("matchDetails");
        Set<String> active = new HashSet<>();
        if (cache == null) {
            return active;
        }
        for (Match match : recentMatches) {
            MatchDetailsResponse details = cache.get(match.getId(), MatchDetailsResponse.class);
            if (details == null) {
                continue; // not cached yet - warmMatchDetails will populate it in the background
            }
            for (GameDetails game : details.games()) {
                collectRosterNames(active, game.blueTeam(), teamId);
                collectRosterNames(active, game.redTeam(), teamId);
            }
        }
        return active;
    }

    /**
     * Fire-and-forget: pre-warms the matchDetails cache for a team's recent matches so the *next*
     * findActiveSummonerNames call (the delayed frontend refetch - see useTeamDetail) has real data
     * instead of cache misses. Safe to call on every page load - MatchDetailsService's own cache
     * means an already-warm match is a no-op here.
     */
    @Async
    public void warmMatchDetails(List<Match> recentMatches) {
        for (Match match : recentMatches) {
            try {
                matchDetailsService.getDetails(match.getId());
            } catch (ExternalApiException ex) {
                log.warn("Could not warm match details for {}: {}", match.getId(), ex.toString());
            }
        }
    }

    private static void collectRosterNames(Set<String> names, TeamGameDetails teamDetails, UUID teamId) {
        if (teamDetails == null || !teamId.equals(teamDetails.teamId())) {
            return;
        }
        teamDetails.players().forEach(p -> names.add(normalizeSummonerName(p.summonerName())));
    }

    /** Riot's roster and live-stats summoner names can differ only by whitespace/case (seen live: "Cloud "). */
    private static String normalizeSummonerName(String summonerName) {
        return summonerName == null ? "" : summonerName.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Live-stats summoner names carry a team-code prefix the roster doesn't ("G2 BrokenBlade" vs
     * roster's "BrokenBlade") - confirmed by direct probing, not documented anywhere. A suffix match
     * (exact, or preceded by a space) handles that prefix without needing the team's code on hand.
     */
    public static boolean isActive(Set<String> liveSummonerNames, String rosterSummonerName) {
        String needle = normalizeSummonerName(rosterSummonerName);
        if (needle.isEmpty()) {
            return false;
        }
        return liveSummonerNames.stream()
                .anyMatch(name -> name.equals(needle) || name.endsWith(" " + needle));
    }
}
