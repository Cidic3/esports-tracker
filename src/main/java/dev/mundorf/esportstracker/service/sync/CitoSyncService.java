package dev.mundorf.esportstracker.service.sync;

import dev.mundorf.esportstracker.client.cito.CitoClient;
import dev.mundorf.esportstracker.client.cito.dto.CitoAlgsEvent;
import dev.mundorf.esportstracker.client.cito.dto.CitoAlgsEvent.CitoGameScore;
import dev.mundorf.esportstracker.client.cito.dto.CitoAlgsEvent.CitoTeamScore;
import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.ApexGameResult;
import dev.mundorf.esportstracker.model.entity.ApexMatchDay;
import dev.mundorf.esportstracker.model.entity.ApexTeamResult;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.repository.ApexGameResultRepository;
import dev.mundorf.esportstracker.repository.ApexMatchDayRepository;
import dev.mundorf.esportstracker.repository.ApexTeamResultRepository;
import dev.mundorf.esportstracker.repository.GameRepository;
import dev.mundorf.esportstracker.repository.LeagueRepository;
import dev.mundorf.esportstracker.repository.TeamRepository;
import dev.mundorf.esportstracker.repository.TournamentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reconciles ALGS (Apex Legends) data from the Cito API into our entities. Mirrors
 * RiotSyncService's upsert-by-externalId approach, with two Apex-specific differences:
 * <ul>
 *   <li><b>One API call per cycle.</b> Cito's free tier allows 500 calls/month; the events list
 *       already embeds schedules AND full results, so sync never fans out into per-event calls
 *       the way Riot sync does. That also means Apex runs ONLY on the slow tournaments-cron
 *       (6h, ~120 calls/month) - a Riot-style 15-min cadence would exhaust the budget in days.</li>
 *   <li><b>Leagues are derived, not fetched.</b> Cito has no league catalog; the five ALGS
 *       regions (Americas/EMEA/APAC North/APAC South/Global) are mapped to League rows here,
 *       so ALGS regions plug into the existing league-follow model exactly like LEC/LCK.</li>
 * </ul>
 */
@Service
public class CitoSyncService {

    private static final Logger log = LoggerFactory.getLogger(CitoSyncService.class);
    private static final String APEX_GAME_SLUG = "apex-legends";

    // Cito's region strings -> our League identity. Global maps to region INTERNATIONAL so
    // TournamentTier.forLeague derives INTERNATIONAL for cross-region Playoffs/EWC events,
    // matching how Riot's Worlds/MSI leagues work.
    private record AlgsRegion(String slug, String name, String region) {
    }

    private static final Map<String, AlgsRegion> ALGS_REGIONS = Map.of(
            "Americas", new AlgsRegion("algs-americas", "ALGS Americas", "AMERICAS"),
            "Europe Middle East and Africa", new AlgsRegion("algs-emea", "ALGS EMEA", "EMEA"),
            "Asia Pacific North", new AlgsRegion("algs-apac-north", "ALGS APAC North", "APAC NORTH"),
            "Asia Pacific South", new AlgsRegion("algs-apac-south", "ALGS APAC South", "APAC SOUTH"),
            "Global", new AlgsRegion("algs-global", "ALGS Global", "INTERNATIONAL"));

    private final CitoClient client;
    private final GameRepository gameRepository;
    private final LeagueRepository leagueRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final ApexMatchDayRepository matchDayRepository;
    private final ApexTeamResultRepository teamResultRepository;
    private final ApexGameResultRepository gameResultRepository;

    public CitoSyncService(CitoClient client, GameRepository gameRepository, LeagueRepository leagueRepository,
                           TournamentRepository tournamentRepository, TeamRepository teamRepository,
                           ApexMatchDayRepository matchDayRepository,
                           ApexTeamResultRepository teamResultRepository,
                           ApexGameResultRepository gameResultRepository) {
        this.client = client;
        this.gameRepository = gameRepository;
        this.leagueRepository = leagueRepository;
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.matchDayRepository = matchDayRepository;
        this.teamResultRepository = teamResultRepository;
        this.gameResultRepository = gameResultRepository;
    }

    @Transactional
    public void syncAlgsEvents() {
        Game game = apexGame();
        List<CitoAlgsEvent> events = client.getAlgsEvents();
        log.info("Syncing {} ALGS events from Cito", events.size());

        for (CitoAlgsEvent event : events) {
            try {
                syncEvent(game, event);
            } catch (Exception ex) {
                // One event's failure shouldn't abort the rest of the poll.
                log.warn("Skipping ALGS event {}: {}", event.id(), ex.toString());
            }
        }
        log.info("ALGS sync complete");
    }

    void syncEvent(Game game, CitoAlgsEvent event) {
        AlgsRegion regionMapping = ALGS_REGIONS.get(event.region());
        if (regionMapping == null) {
            // A region we don't know (Cito added one, or a typo upstream) - skip rather than
            // inventing a league on the fly; the mapping above is the curated source of truth.
            log.warn("Unknown ALGS region '{}' for event {} - skipping", event.region(), event.id());
            return;
        }
        League league = upsertLeague(game, regionMapping);
        Tournament tournament = upsertTournament(game, league, event);
        ApexMatchDay matchDay = upsertMatchDay(game, tournament, event);
        if (event.statsData() != null && event.statsData().scores() != null) {
            syncResults(game, league, matchDay, event.statsData().scores());
        }
    }

    private League upsertLeague(Game game, AlgsRegion mapping) {
        return leagueRepository.findByGameIdAndExternalId(game.getId(), mapping.slug())
                .map(existing -> {
                    existing.update(mapping.name(), mapping.slug(), mapping.region());
                    return leagueRepository.save(existing);
                })
                .orElseGet(() -> leagueRepository.save(new League(
                        mapping.name(), mapping.slug(), mapping.region(), game, mapping.slug())));
    }

    /**
     * One Tournament per (yearSlug, eventSlug), e.g. "ALGS Year 6 Split 1 Pro League EMEA".
     * Cito has no tournament-level date range, so start/end dates are grown incrementally from
     * the match day dates seen: each event widens its tournament's range if it falls outside it.
     */
    private Tournament upsertTournament(Game game, League league, CitoAlgsEvent event) {
        String externalId = event.yearSlug() + "/" + event.eventSlug();
        String slug = event.yearSlug() + "-" + event.eventSlug();
        String name = "ALGS " + prettifySlug(event.yearSlug()) + " " + prettifySlug(event.eventSlug());
        LocalDate eventDate = event.startsAt().atZone(ZoneOffset.UTC).toLocalDate();
        TournamentTier tier = TournamentTier.forLeague(league.getRegion(), league.getSlug());

        return tournamentRepository.findByGameIdAndExternalId(game.getId(), externalId)
                .map(existing -> {
                    LocalDate start = eventDate.isBefore(existing.getStartDate()) ? eventDate : existing.getStartDate();
                    LocalDate end = eventDate.isAfter(existing.getEndDate()) ? eventDate : existing.getEndDate();
                    existing.update(name, slug, league, start, end, tier,
                            RiotSyncService.deriveStatus(start, end), existing.getPrizePool());
                    return tournamentRepository.save(existing);
                })
                .orElseGet(() -> tournamentRepository.save(new Tournament(
                        name, slug, league, game, eventDate, eventDate, tier,
                        RiotSyncService.deriveStatus(eventDate, eventDate), null, externalId)));
    }

    private ApexMatchDay upsertMatchDay(Game game, Tournament tournament, CitoAlgsEvent event) {
        EventStatus status = deriveDayStatus(event.status(), event.startsAt());
        return matchDayRepository.findByGameIdAndExternalId(game.getId(), event.id())
                .map(existing -> {
                    existing.update(tournament, event.name(), event.startsAt(), status);
                    return matchDayRepository.save(existing);
                })
                .orElseGet(() -> matchDayRepository.save(new ApexMatchDay(
                        tournament, game, event.name(), event.startsAt(), status, event.id())));
    }

    /**
     * Upserts each team's cumulative result plus its per-game breakdown. Team rows are upserted
     * by slugified name - Cito exposes team names only, no stable ids or logos - and get the
     * event's ALGS region as their home league, which is what lets a followed Apex team surface
     * its region's upcoming match days in the feed (see ApexMatchDayService).
     */
    private void syncResults(Game game, League league, ApexMatchDay matchDay, List<CitoTeamScore> scores) {
        for (CitoTeamScore score : scores) {
            if (score.teamName() == null || score.teamName().isBlank()) {
                continue;
            }
            Team team = upsertTeam(game, league, score.teamName());
            ApexTeamResult result = teamResultRepository.findByMatchDayIdAndTeamId(matchDay.getId(), team.getId())
                    .map(existing -> {
                        existing.update(score.rank(), score.totalScore());
                        return teamResultRepository.save(existing);
                    })
                    .orElseGet(() -> teamResultRepository.save(new ApexTeamResult(
                            matchDay, team, score.rank(), score.totalScore())));
            replaceGameResults(result, score.games());
        }
    }

    private Team upsertTeam(Game game, League league, String teamName) {
        String slug = RiotSyncService.slugify(teamName);
        return teamRepository.findByGameIdAndExternalId(game.getId(), slug)
                .map(existing -> {
                    existing.update(teamName, slug, existing.getLogoUrl());
                    existing.assignLeague(league);
                    return teamRepository.save(existing);
                })
                .orElseGet(() -> {
                    Team created = new Team(teamName, slug, null, game, slug);
                    created.assignLeague(league);
                    return teamRepository.save(created);
                });
    }

    /** Same diff-based full replace as roster sync: a game Cito no longer reports actually disappears. */
    private void replaceGameResults(ApexTeamResult result, List<CitoGameScore> games) {
        List<CitoGameScore> scores = games == null ? List.of() : games;
        Map<Integer, ApexGameResult> existingByNumber = gameResultRepository.findByTeamResultId(result.getId())
                .stream()
                .collect(Collectors.toMap(ApexGameResult::getGameNumber, Function.identity()));

        for (CitoGameScore score : scores) {
            ApexGameResult existing = existingByNumber.remove(score.gameNumber());
            if (existing != null) {
                existing.update(score.placement(), score.kills(), score.points());
                gameResultRepository.save(existing);
            } else {
                gameResultRepository.save(new ApexGameResult(
                        result, score.gameNumber(), score.placement(), score.kills(), score.points()));
            }
        }
        if (!existingByNumber.isEmpty()) {
            gameResultRepository.deleteAll(existingByNumber.values());
        }
    }

    private Game apexGame() {
        return gameRepository.findBySlug(APEX_GAME_SLUG)
                .orElseThrow(() -> new ResourceNotFoundException("Seed game missing: " + APEX_GAME_SLUG));
    }

    // Package-private for direct testing, same convention as RiotSyncService's pure helpers.

    // How long after its start a still-"pending" day is given the benefit of the doubt. An ALGS
    // match day runs ~6 games over a few hours, so past this window "pending" can only mean
    // Cito's status is lagging, not that the day hasn't happened.
    private static final Duration STALE_PENDING_GRACE = Duration.ofHours(12);

    /** Cito's status vocabulary is pending/completed; anything unrecognized defaults to UPCOMING. */
    static EventStatus mapStatus(String citoStatus) {
        return "completed".equalsIgnoreCase(citoStatus) ? EventStatus.FINISHED : EventStatus.UPCOMING;
    }

    /**
     * mapStatus plus a staleness correction: Cito sometimes leaves long-past events as "pending"
     * for days (observed live with the Year 6 EWC Playoffs group stage - still "pending" three
     * days after being played). A day whose start is more than STALE_PENDING_GRACE in the past
     * cannot honestly be called upcoming anymore, so it's stored as FINISHED - its results simply
     * stay empty until Cito catches up, which the detail view already handles.
     */
    static EventStatus deriveDayStatus(String citoStatus, Instant startsAt) {
        EventStatus status = mapStatus(citoStatus);
        if (status == EventStatus.UPCOMING && startsAt.plus(STALE_PENDING_GRACE).isBefore(Instant.now())) {
            return EventStatus.FINISHED;
        }
        return status;
    }

    /** "split-1-pro-league-emea" -> "Split 1 Pro League EMEA"; known acronyms stay uppercase. */
    static String prettifySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return "";
        }
        String[] parts = slug.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (part.equalsIgnoreCase("emea") || part.equalsIgnoreCase("apac") || part.equalsIgnoreCase("ewc")) {
                sb.append(part.toUpperCase(Locale.ROOT));
            } else {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
