package dev.mundorf.esportstracker.service.sync;

import dev.mundorf.esportstracker.client.cito.CitoClient;
import dev.mundorf.esportstracker.client.cito.dto.CitoAlgsEvent;
import dev.mundorf.esportstracker.client.cito.dto.CitoAlgsEvent.CitoGameScore;
import dev.mundorf.esportstracker.client.cito.dto.CitoAlgsEvent.CitoStatsData;
import dev.mundorf.esportstracker.client.cito.dto.CitoAlgsEvent.CitoTeamScore;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitoSyncServiceTest {

    @Mock
    private CitoClient client;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private LeagueRepository leagueRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private ApexMatchDayRepository matchDayRepository;
    @Mock
    private ApexTeamResultRepository teamResultRepository;
    @Mock
    private ApexGameResultRepository gameResultRepository;

    private CitoSyncService syncService;
    private Game apexGame;

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    @BeforeEach
    void setUp() {
        syncService = new CitoSyncService(client, gameRepository, leagueRepository, tournamentRepository,
                teamRepository, matchDayRepository, teamResultRepository, gameResultRepository);
        apexGame = withId(new Game("Apex Legends", "apex-legends", null));
    }

    // ---- Pure function tests: fast, exhaustive, no mocking needed ----

    @ParameterizedTest
    @CsvSource({
            "completed, FINISHED",
            "COMPLETED, FINISHED",
            "pending,   UPCOMING",
            "somethingCitoAddsLater, UPCOMING" // unrecognized state defaults safely, doesn't throw
    })
    void shouldMapCitoStatusToEventStatus(String citoStatus, EventStatus expected) {
        assertThat(CitoSyncService.mapStatus(citoStatus)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    void shouldMapNullStatusToUpcoming(String citoStatus) {
        assertThat(CitoSyncService.mapStatus(citoStatus)).isEqualTo(EventStatus.UPCOMING);
    }

    // Cito can leave long-past events as "pending" for days (seen live with the EWC Playoffs
    // group stage) - a stale pending day must not keep showing as upcoming.
    @Test
    void shouldDeriveFinishedForPendingDayWhoseStartIsWellPast() {
        Instant threeDaysAgo = Instant.now().minus(3, java.time.temporal.ChronoUnit.DAYS);
        assertThat(CitoSyncService.deriveDayStatus("pending", threeDaysAgo)).isEqualTo(EventStatus.FINISHED);
    }

    @Test
    void shouldKeepPendingDayUpcomingWithinTheGraceWindow() {
        // A day that started two hours ago may genuinely still be in progress (~6 games over a
        // few hours) - don't flip it to FINISHED prematurely.
        Instant twoHoursAgo = Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS);
        assertThat(CitoSyncService.deriveDayStatus("pending", twoHoursAgo)).isEqualTo(EventStatus.UPCOMING);
    }

    @Test
    void shouldKeepFutureUpcomingAndPastCompletedUnchanged() {
        Instant tomorrow = Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS);
        assertThat(CitoSyncService.deriveDayStatus("pending", tomorrow)).isEqualTo(EventStatus.UPCOMING);
        Instant lastWeek = Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
        assertThat(CitoSyncService.deriveDayStatus("completed", lastWeek)).isEqualTo(EventStatus.FINISHED);
    }

    @ParameterizedTest
    @CsvSource({
            "split-1-pro-league-emea,       Split 1 Pro League EMEA",
            "split-1-playoffs-ewc,          Split 1 Playoffs EWC",
            "year-6,                        Year 6",
            "split-1-pro-league-apac-south, Split 1 Pro League APAC South"
    })
    void shouldPrettifyEventSlug(String slug, String expected) {
        assertThat(CitoSyncService.prettifySlug(slug)).isEqualTo(expected);
    }

    // ---- Orchestration tests: mocked client + repositories, verifying the upsert decisions ----

    // Relative future date, not a hardcoded one - deriveDayStatus flips stale pending days to
    // FINISHED, so a fixed date would silently change this fixture's meaning once it passed.
    private static final Instant FUTURE_START = Instant.now().plus(10, java.time.temporal.ChronoUnit.DAYS);

    private CitoAlgsEvent pendingEvent(String id, String region) {
        return new CitoAlgsEvent(id, "Group A vs B", "Split 1", region,
                FUTURE_START, "pending", "year-6", "split-1-pro-league-emea", null);
    }

    private void stubLeagueAndTournamentUpserts() {
        when(leagueRepository.findByGameIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> withId(inv.getArgument(0)));
        when(tournamentRepository.findByGameIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> withId(inv.getArgument(0)));
        when(matchDayRepository.findByGameIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(matchDayRepository.save(any(ApexMatchDay.class))).thenAnswer(inv -> withId(inv.getArgument(0)));
    }

    @Test
    void shouldCreateLeagueTournamentAndMatchDayForNewEvent() {
        stubLeagueAndTournamentUpserts();

        syncService.syncEvent(apexGame, pendingEvent("E1", "Europe Middle East and Africa"));

        ArgumentCaptor<League> leagueCaptor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository).save(leagueCaptor.capture());
        League league = leagueCaptor.getValue();
        assertThat(league.getSlug()).isEqualTo("algs-emea");
        assertThat(league.getName()).isEqualTo("ALGS EMEA");
        assertThat(league.getRegion()).isEqualTo("EMEA");

        ArgumentCaptor<Tournament> tournamentCaptor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(tournamentCaptor.capture());
        Tournament tournament = tournamentCaptor.getValue();
        assertThat(tournament.getName()).isEqualTo("ALGS Year 6 Split 1 Pro League EMEA");
        assertThat(tournament.getExternalId()).isEqualTo("year-6/split-1-pro-league-emea");
        assertThat(tournament.getTier()).isEqualTo(TournamentTier.PRIMARY); // algs-emea is a primary league

        ArgumentCaptor<ApexMatchDay> dayCaptor = ArgumentCaptor.forClass(ApexMatchDay.class);
        verify(matchDayRepository).save(dayCaptor.capture());
        ApexMatchDay day = dayCaptor.getValue();
        assertThat(day.getName()).isEqualTo("Group A vs B");
        assertThat(day.getStatus()).isEqualTo(EventStatus.UPCOMING);
        assertThat(day.getExternalId()).isEqualTo("E1");
    }

    @Test
    void shouldDeriveInternationalTierForGlobalRegion() {
        stubLeagueAndTournamentUpserts();
        CitoAlgsEvent event = new CitoAlgsEvent("E2", "Group A vs B", "Split 1 Playoffs", "Global",
                Instant.parse("2026-07-07T10:30:00Z"), "pending", "year-6", "split-1-playoffs-ewc", null);

        syncService.syncEvent(apexGame, event);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        assertThat(captor.getValue().getTier()).isEqualTo(TournamentTier.INTERNATIONAL);
    }

    @Test
    void shouldSkipEventWithUnknownRegionRatherThanInventingALeague() {
        syncService.syncEvent(apexGame, pendingEvent("E3", "Atlantis"));

        verify(leagueRepository, never()).save(any());
        verify(matchDayRepository, never()).save(any());
    }

    @Test
    void shouldWidenTournamentDateRangeAsNewEventDatesArrive() {
        League league = withId(new League("ALGS EMEA", "algs-emea", "EMEA", apexGame, "algs-emea"));
        when(leagueRepository.findByGameIdAndExternalId(apexGame.getId(), "algs-emea"))
                .thenReturn(Optional.of(league));
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));
        Tournament existing = withId(new Tournament("ALGS Year 6 Split 1 Pro League EMEA",
                "year-6-split-1-pro-league-emea", league, apexGame,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "year-6/split-1-pro-league-emea"));
        when(tournamentRepository.findByGameIdAndExternalId(apexGame.getId(), "year-6/split-1-pro-league-emea"))
                .thenReturn(Optional.of(existing));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchDayRepository.findByGameIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(matchDayRepository.save(any(ApexMatchDay.class))).thenAnswer(inv -> withId(inv.getArgument(0)));

        // FUTURE_START falls after the tournament's current end date of 2026-07-10
        syncService.syncEvent(apexGame, pendingEvent("E4", "Europe Middle East and Africa"));

        verify(tournamentRepository).save(existing); // same instance updated, not a new Tournament
        assertThat(existing.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(existing.getEndDate())
                .isEqualTo(FUTURE_START.atZone(java.time.ZoneOffset.UTC).toLocalDate());
    }

    @Test
    void shouldUpdateExistingMatchDayInPlaceRatherThanInsertingDuplicate() {
        League league = withId(new League("ALGS EMEA", "algs-emea", "EMEA", apexGame, "algs-emea"));
        when(leagueRepository.findByGameIdAndExternalId(apexGame.getId(), "algs-emea"))
                .thenReturn(Optional.of(league));
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));
        Tournament tournament = withId(new Tournament("ALGS Year 6 Split 1 Pro League EMEA",
                "year-6-split-1-pro-league-emea", league, apexGame,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 10),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "year-6/split-1-pro-league-emea"));
        when(tournamentRepository.findByGameIdAndExternalId(apexGame.getId(), "year-6/split-1-pro-league-emea"))
                .thenReturn(Optional.of(tournament));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

        ApexMatchDay existing = withId(new ApexMatchDay(tournament, apexGame, "Group A vs B",
                Instant.parse("2026-08-01T10:00:00Z"), EventStatus.UPCOMING, "E5"));
        when(matchDayRepository.findByGameIdAndExternalId(apexGame.getId(), "E5"))
                .thenReturn(Optional.of(existing));
        when(matchDayRepository.save(any(ApexMatchDay.class))).thenAnswer(inv -> inv.getArgument(0));

        // Same event, now completed
        CitoAlgsEvent event = new CitoAlgsEvent("E5", "Group A vs B", "Split 1",
                "Europe Middle East and Africa", Instant.parse("2026-08-01T10:00:00Z"),
                "completed", "year-6", "split-1-pro-league-emea", null);

        syncService.syncEvent(apexGame, event);

        verify(matchDayRepository).save(existing); // same instance updated, not a new ApexMatchDay
        assertThat(existing.getStatus()).isEqualTo(EventStatus.FINISHED);
    }

    @Test
    void shouldUpsertTeamAndResultsFromStatsData() {
        stubLeagueAndTournamentUpserts();
        when(teamRepository.findByGameIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> withId(inv.getArgument(0)));
        when(teamResultRepository.findByMatchDayIdAndTeamId(any(), any())).thenReturn(Optional.empty());
        when(teamResultRepository.save(any(ApexTeamResult.class))).thenAnswer(inv -> withId(inv.getArgument(0)));
        when(gameResultRepository.findByTeamResultId(any())).thenReturn(List.of());
        when(gameResultRepository.save(any(ApexGameResult.class))).thenAnswer(inv -> inv.getArgument(0));

        CitoStatsData stats = new CitoStatsData(List.of(new CitoTeamScore(1, "VK GAMING", 72, List.of(
                new CitoGameScore(1, 2, 10, 19),
                new CitoGameScore(2, 18, 1, 1)))));
        CitoAlgsEvent event = new CitoAlgsEvent("E6", "Group A vs B", "Split 1",
                "Europe Middle East and Africa", Instant.parse("2026-08-01T10:00:00Z"),
                "completed", "year-6", "split-1-pro-league-emea", stats);

        syncService.syncEvent(apexGame, event);

        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(teamCaptor.capture());
        Team team = teamCaptor.getValue();
        assertThat(team.getName()).isEqualTo("VK GAMING");
        assertThat(team.getSlug()).isEqualTo("vk-gaming"); // slugified name doubles as externalId
        assertThat(team.getLeague()).isNotNull(); // home league stamped for feed team-follow resolution

        ArgumentCaptor<ApexTeamResult> resultCaptor = ArgumentCaptor.forClass(ApexTeamResult.class);
        verify(teamResultRepository).save(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getRank()).isEqualTo(1);
        assertThat(resultCaptor.getValue().getTotalPoints()).isEqualTo(72);

        ArgumentCaptor<ApexGameResult> gameCaptor = ArgumentCaptor.forClass(ApexGameResult.class);
        verify(gameResultRepository, times(2)).save(gameCaptor.capture());
        assertThat(gameCaptor.getAllValues()).extracting(ApexGameResult::getGameNumber).containsExactly(1, 2);
        assertThat(gameCaptor.getAllValues()).extracting(ApexGameResult::getKills).containsExactly(10, 1);
    }

    @Test
    void shouldUpdateExistingGameResultInPlaceAndDeleteRemovedOnes() {
        stubLeagueAndTournamentUpserts();
        Team team = withId(new Team("VK GAMING", "vk-gaming", null, apexGame, "vk-gaming"));
        when(teamRepository.findByGameIdAndExternalId(any(), any())).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        ApexTeamResult existingResult = withId(new ApexTeamResult(null, team, 3, 50));
        when(teamResultRepository.findByMatchDayIdAndTeamId(any(), any())).thenReturn(Optional.of(existingResult));
        when(teamResultRepository.save(any(ApexTeamResult.class))).thenAnswer(inv -> inv.getArgument(0));

        ApexGameResult keptGame = withId(new ApexGameResult(existingResult, 1, 5, 4, 8));
        ApexGameResult staleGame = withId(new ApexGameResult(existingResult, 2, 10, 2, 3));
        when(gameResultRepository.findByTeamResultId(existingResult.getId()))
                .thenReturn(List.of(keptGame, staleGame));
        when(gameResultRepository.save(any(ApexGameResult.class))).thenAnswer(inv -> inv.getArgument(0));

        // Cito now reports only game 1, with corrected numbers
        CitoStatsData stats = new CitoStatsData(List.of(new CitoTeamScore(1, "VK GAMING", 72,
                List.of(new CitoGameScore(1, 2, 10, 19)))));
        CitoAlgsEvent event = new CitoAlgsEvent("E7", "Group A vs B", "Split 1",
                "Europe Middle East and Africa", Instant.parse("2026-08-01T10:00:00Z"),
                "completed", "year-6", "split-1-pro-league-emea", stats);

        syncService.syncEvent(apexGame, event);

        verify(gameResultRepository).save(keptGame); // same instance updated in place
        assertThat(keptGame.getPlacement()).isEqualTo(2);
        assertThat(keptGame.getKills()).isEqualTo(10);
        // deleteAll receives a Map.values() view, so capture rather than comparing to a List
        ArgumentCaptor<Iterable<ApexGameResult>> deletedCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(gameResultRepository).deleteAll(deletedCaptor.capture()); // game 2 disappeared upstream
        assertThat(deletedCaptor.getValue()).containsExactly(staleGame);

        verify(teamResultRepository).save(existingResult);
        assertThat(existingResult.getRank()).isEqualTo(1);
        assertThat(existingResult.getTotalPoints()).isEqualTo(72);
    }

    @Test
    void shouldContinueSyncingRemainingEventsAfterOneFails() {
        when(gameRepository.findBySlug("apex-legends")).thenReturn(Optional.of(apexGame));
        when(client.getAlgsEvents()).thenReturn(List.of(
                pendingEvent("E-FAIL", "Europe Middle East and Africa"),
                pendingEvent("E-OK", "Americas")));
        // First event's league lookup blows up; second event should still be processed.
        when(leagueRepository.findByGameIdAndExternalId(apexGame.getId(), "algs-emea"))
                .thenThrow(new RuntimeException("boom"));
        when(leagueRepository.findByGameIdAndExternalId(apexGame.getId(), "algs-americas"))
                .thenReturn(Optional.empty());
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> withId(inv.getArgument(0)));
        when(tournamentRepository.findByGameIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> withId(inv.getArgument(0)));
        when(matchDayRepository.findByGameIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(matchDayRepository.save(any(ApexMatchDay.class))).thenAnswer(inv -> withId(inv.getArgument(0)));

        syncService.syncAlgsEvents();

        ArgumentCaptor<ApexMatchDay> captor = ArgumentCaptor.forClass(ApexMatchDay.class);
        verify(matchDayRepository).save(captor.capture());
        assertThat(captor.getValue().getExternalId()).isEqualTo("E-OK");
    }
}
