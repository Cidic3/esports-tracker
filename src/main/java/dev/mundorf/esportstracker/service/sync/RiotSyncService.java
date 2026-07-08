package dev.mundorf.esportstracker.service.sync;

import dev.mundorf.esportstracker.client.riot.RiotEsportsClient;
import dev.mundorf.esportstracker.client.riot.dto.RiotEventDetail;
import dev.mundorf.esportstracker.client.riot.dto.RiotEventTeam;
import dev.mundorf.esportstracker.client.riot.dto.RiotLeague;
import dev.mundorf.esportstracker.client.riot.dto.RiotScheduleEvent;
import dev.mundorf.esportstracker.client.riot.dto.RiotStandingEntry;
import dev.mundorf.esportstracker.client.riot.dto.RiotTournament;
import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Standing;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.repository.GameRepository;
import dev.mundorf.esportstracker.repository.LeagueRepository;
import dev.mundorf.esportstracker.repository.MatchRepository;
import dev.mundorf.esportstracker.repository.StandingRepository;
import dev.mundorf.esportstracker.repository.TeamRepository;
import dev.mundorf.esportstracker.repository.TournamentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pulls League of Legends data from Riot's esports API and reconciles it into our entities.
 *
 * <p>Reconciliation is upsert-by-externalId, never delete-and-recreate: existing rows are updated
 * in place, new ones inserted. This is idempotent (re-syncing unchanged data is a harmless no-op)
 * and self-healing (a corrected score on Riot's side is picked up on the next poll).
 *
 * <p>Deliberately NOT wrapped in one big transaction: each save auto-commits on its own, so a
 * mid-sync failure leaves already-synced rows committed (partial progress is preserved and the
 * next run continues) rather than rolling back an hour of work. It also keeps DB connections off
 * the HTTP call path.
 */
@Service
public class RiotSyncService {

    private static final Logger log = LoggerFactory.getLogger(RiotSyncService.class);

    private static final String LOL_GAME_SLUG = "league-of-legends";
    private static final Set<String> PRIMARY_LEAGUE_SLUGS = Set.of("lec", "lck", "lpl", "lcs");
    private static final String REGION_INTERNATIONAL = "INTERNATIONAL";
    private static final int MATCH_SYNC_HORIZON_DAYS = 14;

    // For prettifying tournament slugs into display names. Riot's older seasons named splits
    // winter/spring/summer; newer ones use "split_1/2/3". Map the former onto the latter so names
    // read consistently. winter -> Split 1, spring -> Split 2, summer -> Split 3 (chronological order).
    private static final Map<String, String> SPLIT_TERMS = Map.of(
            "winter", "Split 1",
            "spring", "Split 2",
            "summer", "Split 3");
    private static final Set<String> LEAGUE_ACRONYMS = Set.of(
            "lec", "lck", "lpl", "lcs", "lcp", "msi", "nacl", "cblol", "ewc", "lla",
            "pcs", "vcs", "ljl", "lco", "tcl", "ti", "na", "eu");

    private final RiotEsportsClient client;
    private final GameRepository gameRepository;
    private final LeagueRepository leagueRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final StandingRepository standingRepository;

    public RiotSyncService(RiotEsportsClient client,
                           GameRepository gameRepository,
                           LeagueRepository leagueRepository,
                           TournamentRepository tournamentRepository,
                           TeamRepository teamRepository,
                           MatchRepository matchRepository,
                           StandingRepository standingRepository) {
        this.client = client;
        this.gameRepository = gameRepository;
        this.leagueRepository = leagueRepository;
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.standingRepository = standingRepository;
    }

    /** Slow-changing metadata: every league Riot exposes, and all tournaments under each. */
    public void syncLeaguesAndTournaments() {
        Game game = lolGame();
        List<RiotLeague> riotLeagues = client.getLeagues();
        log.info("Syncing {} leagues from Riot", riotLeagues.size());

        for (RiotLeague riotLeague : riotLeagues) {
            League league = upsertLeague(game, riotLeague);
            List<RiotTournament> tournaments = client.getTournamentsForLeague(riotLeague.id());
            for (RiotTournament riotTournament : tournaments) {
                upsertTournament(game, league, riotTournament, riotLeague.region());
            }
        }
        log.info("League/tournament sync complete");
    }

    /** Fast-changing data: matches (and their teams) for leagues currently in-season. */
    public void syncMatches() {
        Game game = lolGame();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<League> activeLeagues = tournamentRepository.findLeaguesWithActiveTournaments(
                today, today.plusDays(MATCH_SYNC_HORIZON_DAYS));
        log.info("Syncing matches for {} in-season leagues", activeLeagues.size());

        for (League league : activeLeagues) {
            try {
                syncMatchesForLeague(game, league);
            } catch (Exception ex) {
                // One league's failure shouldn't abort the rest of the poll.
                log.warn("Match sync failed for league {}: {}", league.getSlug(), ex.toString());
            }
        }
    }

    void syncMatchesForLeague(Game game, League league) {
        List<RiotScheduleEvent> events = client.getSchedule(league.getExternalId());
        for (RiotScheduleEvent event : events) {
            if (!"match".equals(event.type()) || event.match() == null) {
                continue; // skip non-match events (e.g. "show" segments)
            }
            try {
                upsertMatchFromEvent(game, event);
            } catch (Exception ex) {
                log.warn("Skipping match {}: {}", event.match().id(), ex.toString());
            }
        }
    }

    /** Fast-changing data: standings for tournaments currently in-season, same horizon as matches. */
    public void syncStandings() {
        Game game = lolGame();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Tournament> activeTournaments = tournamentRepository.findActiveTournaments(
                today, today.plusDays(MATCH_SYNC_HORIZON_DAYS));
        log.info("Syncing standings for {} in-season tournaments", activeTournaments.size());

        for (Tournament tournament : activeTournaments) {
            try {
                syncStandingsForTournament(game, tournament);
            } catch (Exception ex) {
                // One tournament's failure shouldn't abort the rest of the poll.
                log.warn("Standings sync failed for tournament {}: {}", tournament.getSlug(), ex.toString());
            }
        }
    }

    void syncStandingsForTournament(Game game, Tournament tournament) {
        List<RiotStandingEntry> entries = client.getStandings(tournament.getExternalId());
        for (RiotStandingEntry entry : entries) {
            Team team = upsertTeam(game, entry.team());
            upsertStanding(tournament, team, entry);
        }
    }

    private void upsertStanding(Tournament tournament, Team team, RiotStandingEntry entry) {
        standingRepository.findByTournamentIdAndTeamIdAndGroupName(tournament.getId(), team.getId(), entry.groupName())
                .ifPresentOrElse(
                        existing -> {
                            existing.update(entry.rank(), entry.wins(), entry.losses());
                            standingRepository.save(existing);
                        },
                        () -> standingRepository.save(new Standing(
                                tournament, team, entry.groupName(), entry.rank(), entry.wins(), entry.losses())));
    }

    private void upsertMatchFromEvent(Game game, RiotScheduleEvent event) {
        String matchId = event.match().id();
        RiotEventDetail detail = client.getEventDetails(matchId);
        if (detail.teams().size() < 2) {
            return;
        }
        RiotEventTeam a = detail.teams().get(0);
        RiotEventTeam b = detail.teams().get(1);
        if (isPlaceholder(a) || isPlaceholder(b)) {
            return; // TBD bracket slots - no real teams to link yet
        }
        if (detail.tournamentId() == null) {
            return;
        }
        Tournament tournament = tournamentRepository
                .findByGameIdAndExternalId(game.getId(), detail.tournamentId())
                .orElse(null);
        if (tournament == null) {
            // Tournament metadata not synced yet; next 6h league sync will create it.
            return;
        }

        Team teamA = upsertTeam(game, a);
        Team teamB = upsertTeam(game, b);
        EventStatus status = mapMatchStatus(event.state());

        matchRepository.findByGameIdAndExternalId(game.getId(), matchId)
                .ifPresentOrElse(
                        existing -> {
                            existing.update(tournament, teamA, teamB, event.startTime(), status,
                                    a.gameWins(), b.gameWins(), null);
                            matchRepository.save(existing);
                        },
                        () -> matchRepository.save(new Match(tournament, game, teamA, teamB,
                                event.startTime(), status, a.gameWins(), b.gameWins(), null, matchId)));
    }

    private League upsertLeague(Game game, RiotLeague riotLeague) {
        return leagueRepository.findByGameIdAndExternalId(game.getId(), riotLeague.id())
                .map(existing -> {
                    existing.update(riotLeague.name(), riotLeague.slug(), riotLeague.region());
                    return leagueRepository.save(existing);
                })
                .orElseGet(() -> leagueRepository.save(new League(
                        riotLeague.name(), riotLeague.slug(), riotLeague.region(), game, riotLeague.id())));
    }

    private void upsertTournament(Game game, League league, RiotTournament riotTournament, String region) {
        TournamentTier tier = deriveTier(region, league.getSlug());
        EventStatus status = deriveStatus(riotTournament.startDate(), riotTournament.endDate());
        // Riot's tournament payload carries no display name, only a slug - derive a readable one.
        String name = prettifyTournamentName(riotTournament.slug());

        tournamentRepository.findByGameIdAndExternalId(game.getId(), riotTournament.id())
                .ifPresentOrElse(
                        existing -> {
                            existing.update(name, riotTournament.slug(), league, riotTournament.startDate(),
                                    riotTournament.endDate(), tier, status, null);
                            tournamentRepository.save(existing);
                        },
                        () -> tournamentRepository.save(new Tournament(name, riotTournament.slug(), league, game,
                                riotTournament.startDate(), riotTournament.endDate(), tier, status, null,
                                riotTournament.id())));
    }

    private Team upsertTeam(Game game, RiotEventTeam riotTeam) {
        String slug = slugify(riotTeam.name());
        return teamRepository.findByGameIdAndExternalId(game.getId(), riotTeam.id())
                .map(existing -> {
                    existing.update(riotTeam.name(), slug, riotTeam.image());
                    return teamRepository.save(existing);
                })
                .orElseGet(() -> teamRepository.save(new Team(
                        riotTeam.name(), slug, riotTeam.image(), game, riotTeam.id())));
    }

    private Game lolGame() {
        return gameRepository.findBySlug(LOL_GAME_SLUG)
                .orElseThrow(() -> new ResourceNotFoundException("Seed game missing: " + LOL_GAME_SLUG));
    }

    // Package-private (not private): lets RiotSyncServiceTest exercise these pure functions
    // directly, without going through the full sync orchestration just to hit one branch.
    static TournamentTier deriveTier(String region, String leagueSlug) {
        if (REGION_INTERNATIONAL.equalsIgnoreCase(region)) {
            return TournamentTier.INTERNATIONAL;
        }
        if (PRIMARY_LEAGUE_SLUGS.contains(leagueSlug)) {
            return TournamentTier.PRIMARY;
        }
        return TournamentTier.SECONDARY;
    }

    static EventStatus deriveStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (today.isBefore(startDate)) {
            return EventStatus.UPCOMING;
        }
        if (today.isAfter(endDate)) {
            return EventStatus.FINISHED;
        }
        return EventStatus.RUNNING;
    }

    static EventStatus mapMatchStatus(String riotState) {
        return switch (riotState == null ? "" : riotState) {
            case "inProgress" -> EventStatus.RUNNING;
            case "completed" -> EventStatus.FINISHED;
            default -> EventStatus.UPCOMING; // "unstarted" and anything unrecognized
        };
    }

    static boolean isPlaceholder(RiotEventTeam team) {
        return team.id() == null || team.id().isBlank() || "0".equals(team.id())
                || team.name() == null || "TBD".equalsIgnoreCase(team.name());
    }

    /**
     * Turns a tournament slug ("lec_summer_2025", "lec_split_3_2026") into a readable name
     * ("LEC Split 3 2025", "LEC Split 3 2026"). Best-effort cosmetic mapping: known league
     * acronyms are upper-cased, winter/spring/summer normalize to Split 1/2/3, "split N" is kept,
     * years pass through, everything else is capitalized.
     */
    static String prettifyTournamentName(String slug) {
        String[] tokens = slug.split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            String piece;
            if (token.isEmpty()) {
                continue;
            } else if (SPLIT_TERMS.containsKey(token)) {
                piece = SPLIT_TERMS.get(token);
            } else if (token.equals("split") && i + 1 < tokens.length && tokens[i + 1].matches("\\d+")) {
                piece = "Split " + tokens[i + 1];
                i++; // consume the number token
            } else if (LEAGUE_ACRONYMS.contains(token)) {
                piece = token.toUpperCase(Locale.ROOT);
            } else if (token.matches("\\d+")) {
                piece = token;
            } else {
                piece = Character.toUpperCase(token.charAt(0)) + token.substring(1);
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(piece);
        }
        return result.toString();
    }

    static String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
