package dev.mundorf.esportstracker.service.sync;

import dev.mundorf.esportstracker.client.riot.RiotEsportsClient;
import dev.mundorf.esportstracker.client.riot.dto.RiotEventDetail;
import dev.mundorf.esportstracker.client.riot.dto.RiotEventLeague;
import dev.mundorf.esportstracker.client.riot.dto.RiotEventTeam;
import dev.mundorf.esportstracker.client.riot.dto.RiotLeague;
import dev.mundorf.esportstracker.client.riot.dto.RiotMatch;
import dev.mundorf.esportstracker.client.riot.dto.RiotScheduleEvent;
import dev.mundorf.esportstracker.client.riot.dto.RiotStandingEntry;
import dev.mundorf.esportstracker.client.riot.dto.RiotTournament;
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
import dev.mundorf.esportstracker.repository.OrganizationRepository;
import dev.mundorf.esportstracker.repository.PlayerRepository;
import dev.mundorf.esportstracker.repository.StandingRepository;
import dev.mundorf.esportstracker.repository.TeamRepository;
import dev.mundorf.esportstracker.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiotSyncServiceTest {

    @Mock
    private RiotEsportsClient client;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private LeagueRepository leagueRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private StandingRepository standingRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private PlayerRepository playerRepository;

    private RiotSyncService syncService;
    private Game lolGame;

    @BeforeEach
    void setUp() {
        syncService = new RiotSyncService(client, gameRepository, leagueRepository, tournamentRepository,
                teamRepository, matchRepository, standingRepository, organizationRepository, playerRepository);
        lolGame = new Game("League of Legends", "league-of-legends", null);
    }

    // ---- Pure function tests: fast, exhaustive, no mocking needed ----

    @ParameterizedTest(name = "region={0}, leagueSlug={1} -> {2}")
    @CsvSource({
            "INTERNATIONAL, worlds, INTERNATIONAL",
            "international, msi, INTERNATIONAL",  // case-insensitive
            "EMEA,          lec,    PRIMARY",
            "KOREA,         lck,    PRIMARY",
            "NORTH AMERICA, lcs,    PRIMARY",
            "EMEA,          lfl,    SECONDARY",   // regional minor league, not in the major set
            "BRAZIL,        cblol,  SECONDARY"
    })
    void shouldDeriveTournamentTier(String region, String leagueSlug, TournamentTier expected) {
        assertThat(RiotSyncService.deriveTier(region, leagueSlug)).isEqualTo(expected);
    }

    @Test
    void shouldDeriveUpcomingStatusForFutureTournament() {
        LocalDate future = LocalDate.now().plusDays(10);
        assertThat(RiotSyncService.deriveStatus(future, future.plusDays(30))).isEqualTo(EventStatus.UPCOMING);
    }

    @Test
    void shouldDeriveRunningStatusForCurrentTournament() {
        LocalDate today = LocalDate.now();
        assertThat(RiotSyncService.deriveStatus(today.minusDays(5), today.plusDays(5)))
                .isEqualTo(EventStatus.RUNNING);
    }

    @Test
    void shouldDeriveFinishedStatusForPastTournament() {
        LocalDate past = LocalDate.now().minusDays(30);
        assertThat(RiotSyncService.deriveStatus(past, past.plusDays(10))).isEqualTo(EventStatus.FINISHED);
    }

    @ParameterizedTest
    @CsvSource({
            "inProgress, RUNNING",
            "completed,  FINISHED",
            "unstarted,  UPCOMING",
            "somethingRiotAddsLater, UPCOMING" // unrecognized state defaults safely, doesn't throw
    })
    void shouldMapRiotMatchStateToEventStatus(String riotState, EventStatus expected) {
        assertThat(RiotSyncService.mapMatchStatus(riotState)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    void shouldMapNullStateToUpcoming(String riotState) {
        assertThat(RiotSyncService.mapMatchStatus(riotState)).isEqualTo(EventStatus.UPCOMING);
    }

    @ParameterizedTest
    @CsvSource({
            "lec_winter_2025,       LEC Winter 2025",
            "lec_spring_2025,       LEC Spring 2025",
            "lec_summer_2025,       LEC Summer 2025",
            "lec_split_3_2026,      LEC Summer 2026",
            "lec_split_5_2026,      LEC Split 5 2026",
            "worlds_2026,           Worlds 2026",
            "lec_season_finals_2024, LEC Season Finals 2024"
    })
    void shouldPrettifyTournamentSlugIntoReadableName(String slug, String expectedName) {
        assertThat(RiotSyncService.prettifyTournamentName(slug)).isEqualTo(expectedName);
    }

    @ParameterizedTest
    @CsvSource({
            "Movistar KOI, movistar-koi",
            "G2 Esports,   g2-esports",
            "T1,           t1"
    })
    void shouldSlugifyTeamName(String name, String expectedSlug) {
        assertThat(RiotSyncService.slugify(name)).isEqualTo(expectedSlug);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "0"})
    void shouldTreatBlankOrZeroIdAsPlaceholder(String id) {
        assertThat(RiotSyncService.isPlaceholder(new RiotEventTeam(id, "Some Team", "ST", null, null))).isTrue();
    }

    @Test
    void shouldTreatTbdNameAsPlaceholder() {
        assertThat(RiotSyncService.isPlaceholder(new RiotEventTeam("123", "TBD", "TBD", null, null))).isTrue();
    }

    @Test
    void shouldNotTreatRealTeamAsPlaceholder() {
        assertThat(RiotSyncService.isPlaceholder(new RiotEventTeam("123", "G2 Esports", "G2", null, null))).isFalse();
    }

    // ---- Orchestration tests: mocked client + repositories, verifying the upsert decisions ----

    @Test
    void shouldInsertNewLeagueWhenNotFound() {
        when(gameRepository.findBySlug("league-of-legends")).thenReturn(Optional.of(lolGame));
        RiotLeague riotLeague = new RiotLeague("98767991302996019", "lec", "LEC", "EMEA");
        when(client.getLeagues()).thenReturn(List.of(riotLeague));
        when(leagueRepository.findByGameIdAndExternalId(lolGame.getId(), "98767991302996019"))
                .thenReturn(Optional.empty());
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));
        when(client.getTournamentsForLeague("98767991302996019")).thenReturn(List.of());

        syncService.syncLeaguesAndTournaments();

        ArgumentCaptor<League> captor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository).save(captor.capture());
        League saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("LEC");
        assertThat(saved.getSlug()).isEqualTo("lec");
        assertThat(saved.getRegion()).isEqualTo("EMEA");
        assertThat(saved.getExternalId()).isEqualTo("98767991302996019");
    }

    @Test
    void shouldUpdateExistingLeagueInPlaceRatherThanInsertingDuplicate() {
        when(gameRepository.findBySlug("league-of-legends")).thenReturn(Optional.of(lolGame));
        League existing = new League("LEC", "lec", "EMEA", lolGame, "98767991302996019");
        RiotLeague riotLeague = new RiotLeague("98767991302996019", "lec", "LEC Renamed", "EMEA");
        when(client.getLeagues()).thenReturn(List.of(riotLeague));
        when(leagueRepository.findByGameIdAndExternalId(lolGame.getId(), "98767991302996019"))
                .thenReturn(Optional.of(existing));
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));
        when(client.getTournamentsForLeague("98767991302996019")).thenReturn(List.of());

        syncService.syncLeaguesAndTournaments();

        verify(leagueRepository).save(existing); // same instance updated, not a new League
        assertThat(existing.getName()).isEqualTo("LEC Renamed");
    }

    @Test
    void shouldCreateTournamentLinkedToItsLeagueWithDerivedTierAndStatus() {
        when(gameRepository.findBySlug("league-of-legends")).thenReturn(Optional.of(lolGame));
        RiotLeague riotLeague = new RiotLeague("98767991302996019", "lec", "LEC", "EMEA");
        when(client.getLeagues()).thenReturn(List.of(riotLeague));
        League league = new League("LEC", "lec", "EMEA", lolGame, "98767991302996019");
        when(leagueRepository.findByGameIdAndExternalId(lolGame.getId(), "98767991302996019"))
                .thenReturn(Optional.of(league));
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate future = LocalDate.now().plusDays(10);
        RiotTournament riotTournament = new RiotTournament(
                "115548681802226458", "lec_split_3_2026", future, future.plusDays(60));
        when(client.getTournamentsForLeague("98767991302996019")).thenReturn(List.of(riotTournament));
        when(tournamentRepository.findByGameIdAndExternalId(lolGame.getId(), "115548681802226458"))
                .thenReturn(Optional.empty());
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

        syncService.syncLeaguesAndTournaments();

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        Tournament saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("LEC Summer 2026");
        assertThat(saved.getLeague()).isEqualTo(league);
        assertThat(saved.getTier()).isEqualTo(TournamentTier.PRIMARY);
        assertThat(saved.getStatus()).isEqualTo(EventStatus.UPCOMING);
    }

    @Test
    void shouldUpsertMatchWithCorrectTeamsScoresAndTournamentLink() {
        Tournament tournament = new Tournament("LEC Split 2 2026", "lec_split_2_2026",
                new League("LEC", "lec", "EMEA", lolGame, "L1"), lolGame,
                LocalDate.now().minusDays(30), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1");
        when(tournamentRepository.findByGameIdAndExternalId(lolGame.getId(), "T1"))
                .thenReturn(Optional.of(tournament));

        RiotScheduleEvent event = new RiotScheduleEvent(
                Instant.parse("2026-04-06T17:15:00Z"), "completed", "match", "Week 2",
                new RiotEventLeague("LEC", "lec"), new RiotMatch("M1", List.of()));
        when(client.getSchedule("L1")).thenReturn(List.of(event));

        RiotEventTeam teamA = new RiotEventTeam("TA", "Movistar KOI", "MKOI", "logo-a.png", 1);
        RiotEventTeam teamB = new RiotEventTeam("TB", "Team Vitality", "VIT", "logo-b.png", 2);
        when(client.getEventDetails("M1")).thenReturn(new RiotEventDetail("M1", "T1", List.of(teamA, teamB)));

        Team savedTeamA = new Team("Movistar KOI", "movistar-koi", "logo-a.png", lolGame, "TA");
        Team savedTeamB = new Team("Team Vitality", "team-vitality", "logo-b.png", lolGame, "TB");
        when(teamRepository.findByGameIdAndExternalId(lolGame.getId(), "TA")).thenReturn(Optional.empty());
        when(teamRepository.findByGameIdAndExternalId(lolGame.getId(), "TB")).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team t = inv.getArgument(0);
            return t.getExternalId().equals("TA") ? savedTeamA : savedTeamB;
        });
        when(matchRepository.findByGameIdAndExternalId(lolGame.getId(), "M1")).thenReturn(Optional.empty());

        syncService.syncMatchesForLeague(lolGame, new League("LEC", "lec", "EMEA", lolGame, "L1"));

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(captor.capture());
        Match saved = captor.getValue();
        assertThat(saved.getTournament()).isEqualTo(tournament);
        assertThat(saved.getScoreA()).isEqualTo(1);
        assertThat(saved.getScoreB()).isEqualTo(2);
        assertThat(saved.getStatus()).isEqualTo(EventStatus.FINISHED);
    }

    @Test
    void shouldSkipScheduleEventsThatAreNotMatches() {
        RiotScheduleEvent showEvent = new RiotScheduleEvent(
                Instant.now(), "completed", "show", "Pre-show",
                new RiotEventLeague("LEC", "lec"), null);
        when(client.getSchedule("L1")).thenReturn(List.of(showEvent));

        syncService.syncMatchesForLeague(lolGame, new League("LEC", "lec", "EMEA", lolGame, "L1"));

        verify(client, never()).getEventDetails(any());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void shouldSkipMatchWithPlaceholderTeam() {
        RiotScheduleEvent event = new RiotScheduleEvent(
                Instant.now(), "unstarted", "match", "Playoffs",
                new RiotEventLeague("LEC", "lec"), new RiotMatch("M1", List.of()));
        when(client.getSchedule("L1")).thenReturn(List.of(event));
        RiotEventTeam realTeam = new RiotEventTeam("TA", "G2 Esports", "G2", null, null);
        RiotEventTeam tbdTeam = new RiotEventTeam("0", "TBD", "TBD", null, null);
        when(client.getEventDetails("M1")).thenReturn(new RiotEventDetail("M1", "T1", List.of(realTeam, tbdTeam)));

        syncService.syncMatchesForLeague(lolGame, new League("LEC", "lec", "EMEA", lolGame, "L1"));

        verify(matchRepository, never()).save(any());
    }

    @Test
    void shouldSkipMatchWhenTournamentNotYetSynced() {
        RiotScheduleEvent event = new RiotScheduleEvent(
                Instant.now(), "unstarted", "match", "Playoffs",
                new RiotEventLeague("LEC", "lec"), new RiotMatch("M1", List.of()));
        when(client.getSchedule("L1")).thenReturn(List.of(event));
        RiotEventTeam teamA = new RiotEventTeam("TA", "G2 Esports", "G2", null, null);
        RiotEventTeam teamB = new RiotEventTeam("TB", "Fnatic", "FNC", null, null);
        when(client.getEventDetails("M1")).thenReturn(new RiotEventDetail("M1", "NOT_SYNCED_YET", List.of(teamA, teamB)));
        when(tournamentRepository.findByGameIdAndExternalId(lolGame.getId(), "NOT_SYNCED_YET"))
                .thenReturn(Optional.empty());

        syncService.syncMatchesForLeague(lolGame, new League("LEC", "lec", "EMEA", lolGame, "L1"));

        verify(matchRepository, never()).save(any());
    }

    @Test
    void shouldInsertNewStandingWhenNotFound() {
        Tournament tournament = new Tournament("LEC Split 2 2026", "lec_split_2_2026",
                new League("LEC", "lec", "EMEA", lolGame, "L1"), lolGame,
                LocalDate.now().minusDays(30), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1");
        RiotEventTeam riotTeam = new RiotEventTeam("TA", "G2 Esports", "G2", null, null);
        RiotStandingEntry entry = new RiotStandingEntry("Regular Season", 1, riotTeam, 8, 1);
        when(client.getStandings("T1")).thenReturn(List.of(entry));
        Team savedTeam = new Team("G2 Esports", "g2-esports", null, lolGame, "TA");
        when(teamRepository.findByGameIdAndExternalId(lolGame.getId(), "TA")).thenReturn(Optional.of(savedTeam));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        when(standingRepository.findByTournamentIdAndTeamIdAndGroupName(
                tournament.getId(), savedTeam.getId(), "Regular Season")).thenReturn(Optional.empty());

        syncService.syncStandingsForTournament(lolGame, tournament);

        ArgumentCaptor<Standing> captor = ArgumentCaptor.forClass(Standing.class);
        verify(standingRepository).save(captor.capture());
        Standing saved = captor.getValue();
        assertThat(saved.getTournament()).isEqualTo(tournament);
        assertThat(saved.getTeam()).isEqualTo(savedTeam);
        assertThat(saved.getGroupName()).isEqualTo("Regular Season");
        assertThat(saved.getRank()).isEqualTo(1);
        assertThat(saved.getWins()).isEqualTo(8);
        assertThat(saved.getLosses()).isEqualTo(1);
    }

    @Test
    void shouldUpdateExistingStandingInPlaceRatherThanInsertingDuplicate() {
        Tournament tournament = new Tournament("LEC Split 2 2026", "lec_split_2_2026",
                new League("LEC", "lec", "EMEA", lolGame, "L1"), lolGame,
                LocalDate.now().minusDays(30), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1");
        RiotEventTeam riotTeam = new RiotEventTeam("TA", "G2 Esports", "G2", null, null);
        RiotStandingEntry entry = new RiotStandingEntry("Regular Season", 1, riotTeam, 9, 1);
        when(client.getStandings("T1")).thenReturn(List.of(entry));
        Team savedTeam = new Team("G2 Esports", "g2-esports", null, lolGame, "TA");
        when(teamRepository.findByGameIdAndExternalId(lolGame.getId(), "TA")).thenReturn(Optional.of(savedTeam));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        Standing existing = new Standing(tournament, savedTeam, "Regular Season", 1, 8, 1);
        when(standingRepository.findByTournamentIdAndTeamIdAndGroupName(
                tournament.getId(), savedTeam.getId(), "Regular Season")).thenReturn(Optional.of(existing));

        syncService.syncStandingsForTournament(lolGame, tournament);

        verify(standingRepository).save(existing); // same instance updated, not a new Standing
        assertThat(existing.getWins()).isEqualTo(9);
    }
}
