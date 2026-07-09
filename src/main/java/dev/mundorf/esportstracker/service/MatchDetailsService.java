package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.client.riot.RiotEsportsClient;
import dev.mundorf.esportstracker.client.riot.RiotLiveStatsClient;
import dev.mundorf.esportstracker.client.riot.dto.RiotEventGame;
import dev.mundorf.esportstracker.client.riot.dto.RiotGameWindow;
import dev.mundorf.esportstracker.client.riot.dto.RiotParticipantStats;
import dev.mundorf.esportstracker.exception.ExternalApiException;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse.GameDetails;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse.PlayerGameDetails;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse.TeamGameDetails;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * On-demand proxy for per-game match details (champions, items, objectives).
 *
 * <p>Deliberately NOT part of the sync pipeline: everything we sync is catalog data that
 * feeds relational queries (filters, feeds, standings), while game details are display-only,
 * huge (frame-level), and viewed rarely. Fetching at request time with a short cache
 * (see CacheConfig) keeps live games fresher than any polling cadence could, at the cost
 * that a Riot outage degrades this one page instead of nothing — an acceptable trade.
 *
 * <p>Finished games need a "walk": the live stats feed serves timeline frames around a
 * requested startingTime, rejects times too far past the final frame, and only tells you
 * the game is over by returning a frame with gameState "finished". So we start shortly
 * after the game began and step forward until we see that frame (bounded, ~12 calls worst
 * case for a very long game — and the whole result is cached).
 */
@Service
public class MatchDetailsService {

    private static final Logger log = LoggerFactory.getLogger(MatchDetailsService.class);

    private static final String LOL_SLUG = "league-of-legends";
    /** Probes start here after game start — short enough to catch even remake-length games. */
    private static final Duration WALK_INITIAL_OFFSET = Duration.ofMinutes(20);
    private static final Duration WALK_STEP = Duration.ofMinutes(10);
    private static final int WALK_MAX_STEPS = 12;
    /** The feed refuses very recent timestamps; trail live games by a minute. */
    private static final Duration LIVE_LAG = Duration.ofSeconds(60);

    private final MatchService matchService;
    private final RiotEsportsClient esportsClient;
    private final RiotLiveStatsClient liveStatsClient;

    public MatchDetailsService(MatchService matchService,
                               RiotEsportsClient esportsClient,
                               RiotLiveStatsClient liveStatsClient) {
        this.matchService = matchService;
        this.esportsClient = esportsClient;
        this.liveStatsClient = liveStatsClient;
    }

    @Cacheable(cacheNames = "matchDetails", key = "#matchId")
    public MatchDetailsResponse getDetails(UUID matchId) {
        Match match = matchService.findById(matchId);
        // Only the Riot feed is wired up; a Dota 2 equivalent would branch here.
        if (!LOL_SLUG.equals(match.getGame().getSlug())) {
            return new MatchDetailsResponse(matchId, List.of());
        }

        // Riot assigns sides per game by its own team ids; resolve them back to our teams.
        Map<String, Team> teamsByExternalId = new HashMap<>();
        teamsByExternalId.put(match.getTeamA().getExternalId(), match.getTeamA());
        teamsByExternalId.put(match.getTeamB().getExternalId(), match.getTeamB());

        List<GameDetails> games = new ArrayList<>();
        for (RiotEventGame game : esportsClient.getEventGames(match.getExternalId())) {
            if ("unstarted".equals(game.state())) {
                games.add(new GameDetails(game.number(), game.state(), null, null));
            } else {
                games.add(loadGame(game, teamsByExternalId));
            }
        }
        games.sort(Comparator.comparingInt(GameDetails::number));
        return new MatchDetailsResponse(matchId, games);
    }

    /**
     * Stats failures degrade to a game entry without team blocks rather than failing the
     * whole series — one flaky game shouldn't blank the other four.
     */
    private GameDetails loadGame(RiotEventGame game, Map<String, Team> teamsByExternalId) {
        try {
            RiotGameWindow start = liveStatsClient.getWindow(game.id(), null);
            if (start == null) {
                return new GameDetails(game.number(), game.state(), null, null);
            }

            Instant target;
            RiotGameWindow window;
            if ("completed".equals(game.state())) {
                WalkResult end = walkToFinalFrame(game.id(), start.firstFrameTimestamp());
                target = end.time();
                window = end.window() != null ? end.window() : start;
            } else {
                target = Instant.now().minus(LIVE_LAG);
                RiotGameWindow live = liveStatsClient.getWindow(game.id(), target);
                window = live != null ? live : start;
            }

            List<RiotParticipantStats> statLines = liveStatsClient.getDetails(game.id(), target);
            Map<Integer, RiotParticipantStats> statsById = statLines == null ? Map.of()
                    : statLines.stream().collect(Collectors.toMap(
                            RiotParticipantStats::participantId, Function.identity()));

            return new GameDetails(
                    game.number(),
                    game.state(),
                    toTeamDetails(teamsByExternalId.get(game.blueTeamId()), window.blueTeam(), statsById),
                    toTeamDetails(teamsByExternalId.get(game.redTeamId()), window.redTeam(), statsById));
        } catch (ExternalApiException ex) {
            log.warn("Live stats unavailable for game {} ({}): {}", game.id(), game.state(), ex.getMessage());
            return new GameDetails(game.number(), game.state(), null, null);
        }
    }

    private WalkResult walkToFinalFrame(String gameId, Instant firstFrame) {
        Instant time = firstFrame.plus(WALK_INITIAL_OFFSET);
        RiotGameWindow window = null;
        Instant windowTime = null;
        for (int step = 0; step < WALK_MAX_STEPS; step++) {
            RiotGameWindow probe = liveStatsClient.getWindow(gameId, time);
            if (probe == null) {
                break; // overshot the feed's serviceable range; keep the last good frame
            }
            window = probe;
            windowTime = time;
            if ("finished".equals(probe.gameState())) {
                break;
            }
            time = time.plus(WALK_STEP);
        }
        return new WalkResult(window, windowTime);
    }

    private record WalkResult(RiotGameWindow window, Instant time) {
    }

    private static TeamGameDetails toTeamDetails(Team team,
                                                 RiotGameWindow.RiotGameTeamFrame teamFrame,
                                                 Map<Integer, RiotParticipantStats> statsById) {
        List<PlayerGameDetails> players = teamFrame.participants().stream()
                .map(meta -> {
                    RiotParticipantStats stats = statsById.get(meta.participantId());
                    return new PlayerGameDetails(
                            meta.summonerName(),
                            meta.championId(),
                            meta.role(),
                            stats == null ? 0 : stats.level(),
                            stats == null ? 0 : stats.kills(),
                            stats == null ? 0 : stats.deaths(),
                            stats == null ? 0 : stats.assists(),
                            stats == null ? 0 : stats.creepScore(),
                            stats == null ? 0 : stats.totalGoldEarned(),
                            stats == null ? List.of() : stats.items());
                })
                .toList();
        return new TeamGameDetails(
                team == null ? null : team.getId(),
                team == null ? null : team.getName(),
                teamFrame.totalKills(),
                teamFrame.totalGold(),
                teamFrame.towers(),
                teamFrame.barons(),
                teamFrame.dragons(),
                players);
    }
}
