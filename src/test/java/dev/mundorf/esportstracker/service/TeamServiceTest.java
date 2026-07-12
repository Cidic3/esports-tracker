package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ExternalApiException;
import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse.GameDetails;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse.TeamGameDetails;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Player;
import dev.mundorf.esportstracker.model.entity.PlayerRole;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.repository.MatchRepository;
import dev.mundorf.esportstracker.repository.PlayerRepository;
import dev.mundorf.esportstracker.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private TeamSyncTrigger teamSyncTrigger;
    @Mock
    private MatchDetailsService matchDetailsService;
    @Mock
    private CacheManager cacheManager;

    private TeamService teamService;

    private final Game lolGame = withId(new Game("League of Legends", "league-of-legends", null));

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    private TeamService newService() {
        return new TeamService(teamRepository, playerRepository, matchRepository, teamSyncTrigger,
                matchDetailsService, cacheManager);
    }

    // ---- findById: on-demand league sync trigger ----

    @Test
    void shouldTriggerLeagueSyncWhenTeamHasHomeLeague() {
        teamService = newService();
        League lec = withId(new League("LEC", "lec", "EMEA", lolGame, "L1"));
        Team team = withId(new Team("G2 Esports", "g2-esports", null, lolGame, "TA"));
        team.assignLeague(lec);
        UUID id = team.getId();
        when(teamRepository.findWithAssociationsById(id)).thenReturn(Optional.of(team));

        Team result = teamService.findById(id);

        assertThat(result).isSameAs(team);
        verify(teamSyncTrigger).triggerLeagueSync(lec);
    }

    @Test
    void shouldNotTriggerLeagueSyncForApexTeam() {
        // The on-demand trigger hits Riot's schedule endpoint, which knows nothing about ALGS
        // leagues - an Apex team's page must not fire it (see TeamService.findById javadoc).
        teamService = newService();
        Game apexGame = withId(new Game("Apex Legends", "apex-legends", null));
        League algsEmea = withId(new League("ALGS EMEA", "algs-emea", "EMEA", apexGame, "algs-emea"));
        Team team = withId(new Team("VK GAMING", "vk-gaming", null, apexGame, "vk-gaming"));
        team.assignLeague(algsEmea);
        when(teamRepository.findWithAssociationsById(team.getId())).thenReturn(Optional.of(team));

        teamService.findById(team.getId());

        verify(teamSyncTrigger, never()).triggerLeagueSync(any());
    }

    @Test
    void shouldNotTriggerLeagueSyncWhenTeamHasNoHomeLeague() {
        teamService = newService();
        Team team = withId(new Team("G2 Esports", "g2-esports", null, lolGame, "TA"));
        UUID id = team.getId();
        when(teamRepository.findWithAssociationsById(id)).thenReturn(Optional.of(team));

        teamService.findById(id);

        verify(teamSyncTrigger, never()).triggerLeagueSync(any());
    }

    @Test
    void shouldThrowWhenTeamNotFound() {
        teamService = newService();
        UUID id = UUID.randomUUID();
        when(teamRepository.findWithAssociationsById(id)).thenReturn(Optional.empty());

        try {
            teamService.findById(id);
            org.junit.jupiter.api.Assertions.fail("expected ResourceNotFoundException");
        } catch (ResourceNotFoundException expected) {
            // expected
        }
    }

    // ---- findRoster: sorted by lane role ----

    @Test
    void shouldSortRosterByLaneRoleDeclarationOrder() {
        teamService = newService();
        Team team = withId(new Team("G2 Esports", "g2-esports", null, lolGame, "TA"));
        UUID teamId = team.getId();
        Player support = new Player(team, "Support", null, null, null, PlayerRole.SUPPORT, "P1");
        Player top = new Player(team, "Top", null, null, null, PlayerRole.TOP, "P2");
        Player mid = new Player(team, "Mid", null, null, null, PlayerRole.MID, "P3");
        when(playerRepository.findByTeamId(teamId)).thenReturn(List.of(support, top, mid));

        List<Player> roster = teamService.findRoster(teamId);

        assertThat(roster).extracting(Player::getSummonerName).containsExactly("Top", "Mid", "Support");
    }

    // ---- findActiveSummonerNames: heuristic driven by the matchDetails cache ----

    @Test
    void shouldReturnEmptySetWhenMatchDetailsCacheIsAbsent() {
        teamService = newService();
        when(cacheManager.getCache("matchDetails")).thenReturn(null);
        UUID teamId = UUID.randomUUID();
        Match match = mock(Match.class);

        Set<String> active = teamService.findActiveSummonerNames(teamId, List.of(match));

        assertThat(active).isEmpty();
    }

    @Test
    void shouldSkipMatchesNotYetCached() {
        teamService = newService();
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("matchDetails")).thenReturn(cache);
        UUID teamId = UUID.randomUUID();
        Match match = mock(Match.class);
        UUID matchId = UUID.randomUUID();
        when(match.getId()).thenReturn(matchId);
        when(cache.get(matchId, MatchDetailsResponse.class)).thenReturn(null);

        Set<String> active = teamService.findActiveSummonerNames(teamId, List.of(match));

        assertThat(active).isEmpty();
    }

    @Test
    void shouldCollectNormalizedNamesOnlyForRequestedTeamSide() {
        teamService = newService();
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("matchDetails")).thenReturn(cache);
        UUID teamId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        Match match = mock(Match.class);
        UUID matchId = UUID.randomUUID();
        when(match.getId()).thenReturn(matchId);

        TeamGameDetails ourSide = new TeamGameDetails(teamId, "G2 Esports", 10, 50000, 5, 1, List.of(), List.of(
                new MatchDetailsResponse.PlayerGameDetails(" BrokenBlade ", "K'Sante", "top", 15, 3, 1, 5, 200, 12000L, List.of())));
        TeamGameDetails theirSide = new TeamGameDetails(opponentId, "Fnatic", 8, 48000, 3, 0, List.of(), List.of(
                new MatchDetailsResponse.PlayerGameDetails("Oscarinin", "Renekton", "top", 14, 1, 3, 4, 190, 11000L, List.of())));
        GameDetails game = new GameDetails(1, "finished", ourSide, theirSide);
        MatchDetailsResponse details = new MatchDetailsResponse(matchId, List.of(game));
        when(cache.get(matchId, MatchDetailsResponse.class)).thenReturn(details);

        Set<String> active = teamService.findActiveSummonerNames(teamId, List.of(match));

        assertThat(active).containsExactly("brokenblade"); // normalized (trimmed, lowercased); opponent side excluded
    }

    @Test
    void shouldSkipNullTeamGameDetailsSide() {
        teamService = newService();
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("matchDetails")).thenReturn(cache);
        UUID teamId = UUID.randomUUID();
        Match match = mock(Match.class);
        UUID matchId = UUID.randomUUID();
        when(match.getId()).thenReturn(matchId);
        GameDetails game = new GameDetails(1, "finished", null, null); // no stats coverage for this game
        MatchDetailsResponse details = new MatchDetailsResponse(matchId, List.of(game));
        when(cache.get(matchId, MatchDetailsResponse.class)).thenReturn(details);

        Set<String> active = teamService.findActiveSummonerNames(teamId, List.of(match));

        assertThat(active).isEmpty();
    }

    // ---- warmMatchDetails: fire-and-forget, swallows failures per match ----

    @Test
    void shouldContinueWarmingRemainingMatchesAfterOneFails() {
        teamService = newService();
        Match failing = mock(Match.class);
        UUID failingId = UUID.randomUUID();
        when(failing.getId()).thenReturn(failingId);
        Match succeeding = mock(Match.class);
        UUID succeedingId = UUID.randomUUID();
        when(succeeding.getId()).thenReturn(succeedingId);
        when(matchDetailsService.getDetails(failingId))
                .thenThrow(new ExternalApiException("Riot unavailable", new RuntimeException("timeout")));

        teamService.warmMatchDetails(List.of(failing, succeeding));

        verify(matchDetailsService, times(1)).getDetails(failingId);
        verify(matchDetailsService, times(1)).getDetails(succeedingId);
    }

    // ---- isActive: static heuristic, exercised directly without mocking ----

    @ParameterizedTest(name = "live={0}, roster={1} -> {2}")
    @CsvSource({
            "brokenblade,          BrokenBlade,        true",   // exact match, case-insensitive
            "'g2 brokenblade',     BrokenBlade,        true",   // team-code prefix, suffix match
            "'  BrokenBlade  ',    brokenblade,        true",   // whitespace trimmed on both sides
            "caps,                 broken,             false",  // no match
    })
    void shouldMatchActiveHeuristic(String liveName, String rosterName, boolean expected) {
        assertThat(TeamService.isActive(Set.of(liveName.trim().toLowerCase(java.util.Locale.ROOT)), rosterName))
                .isEqualTo(expected);
    }

    @Test
    void shouldTreatBlankOrNullRosterNameAsNotActive() {
        assertThat(TeamService.isActive(Set.of("brokenblade"), "")).isFalse();
        assertThat(TeamService.isActive(Set.of("brokenblade"), null)).isFalse();
        assertThat(TeamService.isActive(Set.of("brokenblade"), "   ")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenNoLiveNamesGiven() {
        assertThat(TeamService.isActive(Set.of(), "brokenblade")).isFalse();
    }

    // ---- match list helpers: correct status/direction delegated to the repository ----

    @Test
    void shouldFetchRecentFinishedMatchesDescending() {
        teamService = newService();
        UUID teamId = UUID.randomUUID();
        Page<Match> page = new PageImpl<>(List.of());
        when(matchRepository.search(eq(null), eq(EventStatus.FINISHED), eq(teamId), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        List<Match> result = teamService.findRecentMatches(teamId);

        assertThat(result).isEmpty();
        verify(matchRepository).search(eq(null), eq(EventStatus.FINISHED), eq(teamId), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void shouldFetchUpcomingMatchesAscending() {
        teamService = newService();
        UUID teamId = UUID.randomUUID();
        when(matchRepository.search(eq(null), eq(EventStatus.UPCOMING), eq(teamId), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        teamService.findUpcomingMatches(teamId);

        verify(matchRepository).search(eq(null), eq(EventStatus.UPCOMING), eq(teamId), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void shouldFetchLiveMatches() {
        teamService = newService();
        UUID teamId = UUID.randomUUID();
        when(matchRepository.search(eq(null), eq(EventStatus.RUNNING), eq(teamId), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        teamService.findLiveMatches(teamId);

        verify(matchRepository).search(eq(null), eq(EventStatus.RUNNING), eq(teamId), eq(null), eq(null), any(Pageable.class));
    }
}
