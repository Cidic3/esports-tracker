package dev.mundorf.esportstracker.service.sync;

import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.repository.GameRepository;
import dev.mundorf.esportstracker.repository.LeagueRepository;
import dev.mundorf.esportstracker.repository.MatchRepository;
import dev.mundorf.esportstracker.repository.StandingRepository;
import dev.mundorf.esportstracker.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end sync against the REAL live Riot API and the REAL configured database. Named *LiveIT so
 * Surefire's default pattern skips it during a normal build (depends on a third-party service + a
 * running Postgres). Run explicitly with all env vars set:
 *   mvn test -Dtest=RiotSyncServiceLiveIT
 * Side effect: it populates the target DB with real LoL data (idempotent - safe to re-run).
 */
@SpringBootTest
class RiotSyncServiceLiveIT {

    @Autowired
    private RiotSyncService syncService;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private StandingRepository standingRepository;

    @Test
    void shouldSyncLeaguesTournamentsAndLecMatches() {
        syncService.syncLeaguesAndTournaments();

        assertThat(leagueRepository.count()).isPositive();
        assertThat(tournamentRepository.count()).isPositive();

        Game lol = gameRepository.findBySlug("league-of-legends").orElseThrow();
        League lec = leagueRepository.findByGameIdAndSlug(lol.getId(), "lec").orElseThrow();

        syncService.syncMatchesForLeague(lol, lec);

        assertThat(matchRepository.count()).isPositive();
    }

    @Test
    void shouldSyncStandingsForAFinishedLecSplit() {
        syncService.syncLeaguesAndTournaments();
        Game lol = gameRepository.findBySlug("league-of-legends").orElseThrow();
        // LEC Split 2 2026 - already FINISHED with a real win/loss record, confirmed manually
        // against the live getStandings response before this feature was built.
        Tournament tournament = tournamentRepository.findByGameIdAndExternalId(lol.getId(), "115548668058343983")
                .orElseThrow();

        syncService.syncStandingsForTournament(lol, tournament);

        var standings = standingRepository.findByTournamentIdOrderByGroupNameAscRankAsc(tournament.getId());
        assertThat(standings).isNotEmpty();
        assertThat(standings).anySatisfy(s -> assertThat(s.getWins() + s.getLosses()).isPositive());
    }
}
