package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchService matchService;

    private final Game lolGame = withId(new Game("League of Legends", "league-of-legends", null));

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    @Test
    void shouldReturnEmptyPageWithoutQueryingWhenUserFollowsNothing() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        Pageable pageable = PageRequest.of(0, 20);

        Page<?> result = matchService.findUpcomingForUser(user, pageable);

        assertThat(result).isEmpty();
        verify(matchRepository, never()).findUpcomingForFollowed(any(), any(), any());
    }

    @Test
    void shouldTreatGameOnlyFollowsAsFollowingNothing() {
        // Game follows are a UI grouping, not a feed driver — a user who follows a game but
        // no leagues/teams gets an empty feed instead of every minor league of that game.
        User user = new User("testuser", "test@example.com", "hashed-password");
        user.replaceFollowedGames(Set.of(lolGame));
        Pageable pageable = PageRequest.of(0, 20);

        Page<?> result = matchService.findUpcomingForUser(user, pageable);

        assertThat(result).isEmpty();
        verify(matchRepository, never()).findUpcomingForFollowed(any(), any(), any());
    }

    @Test
    void shouldQueryByFollowedLeagueIdAndSubstitutePlaceholderForEmptyTeams() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        League lec = withId(new League("LEC", "lec", "EMEA", lolGame, "L1"));
        user.replaceFollowedLeagues(Set.of(lec));
        Pageable pageable = PageRequest.of(0, 20);
        when(matchRepository.findUpcomingForFollowed(any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        matchService.findUpcomingForUser(user, pageable);

        ArgumentCaptor<Set<UUID>> leagueIdsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set<UUID>> teamIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(matchRepository).findUpcomingForFollowed(leagueIdsCaptor.capture(), teamIdsCaptor.capture(), eq(pageable));

        assertThat(leagueIdsCaptor.getValue()).containsExactly(lec.getId());
        assertThat(teamIdsCaptor.getValue()).hasSize(1); // placeholder, since the user follows no teams
    }

    @Test
    void shouldQueryByFollowedTeamId() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        Team g2 = withId(new Team("G2 Esports", "g2-esports", null, lolGame, "TA"));
        user.replaceFollowedTeams(Set.of(g2));
        Pageable pageable = PageRequest.of(0, 20);
        when(matchRepository.findUpcomingForFollowed(any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        matchService.findUpcomingForUser(user, pageable);

        ArgumentCaptor<Set<UUID>> teamIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(matchRepository).findUpcomingForFollowed(any(), teamIdsCaptor.capture(), eq(pageable));
        assertThat(teamIdsCaptor.getValue()).containsExactly(g2.getId());
    }

    // ---- findLiveForUser: same follow semantics as findUpcomingForUser, different repository query ----

    @Test
    void shouldReturnEmptyPageWithoutQueryingLiveMatchesWhenUserFollowsNothing() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        Pageable pageable = PageRequest.of(0, 20);

        Page<?> result = matchService.findLiveForUser(user, pageable);

        assertThat(result).isEmpty();
        verify(matchRepository, never()).findRunningForFollowed(any(), any(), any());
    }

    @Test
    void shouldTreatGameOnlyFollowsAsFollowingNothingForLiveMatches() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        user.replaceFollowedGames(Set.of(lolGame));
        Pageable pageable = PageRequest.of(0, 20);

        Page<?> result = matchService.findLiveForUser(user, pageable);

        assertThat(result).isEmpty();
        verify(matchRepository, never()).findRunningForFollowed(any(), any(), any());
    }

    @Test
    void shouldQueryLiveMatchesByFollowedLeagueIdAndSubstitutePlaceholderForEmptyTeams() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        League lec = withId(new League("LEC", "lec", "EMEA", lolGame, "L1"));
        user.replaceFollowedLeagues(Set.of(lec));
        Pageable pageable = PageRequest.of(0, 20);
        when(matchRepository.findRunningForFollowed(any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        matchService.findLiveForUser(user, pageable);

        ArgumentCaptor<Set<UUID>> leagueIdsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set<UUID>> teamIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(matchRepository).findRunningForFollowed(leagueIdsCaptor.capture(), teamIdsCaptor.capture(), eq(pageable));

        assertThat(leagueIdsCaptor.getValue()).containsExactly(lec.getId());
        assertThat(teamIdsCaptor.getValue()).hasSize(1); // placeholder, since the user follows no teams
    }

    @Test
    void shouldQueryLiveMatchesByFollowedTeamId() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        Team g2 = withId(new Team("G2 Esports", "g2-esports", null, lolGame, "TA"));
        user.replaceFollowedTeams(Set.of(g2));
        Pageable pageable = PageRequest.of(0, 20);
        when(matchRepository.findRunningForFollowed(any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        matchService.findLiveForUser(user, pageable);

        ArgumentCaptor<Set<UUID>> teamIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(matchRepository).findRunningForFollowed(any(), teamIdsCaptor.capture(), eq(pageable));
        assertThat(teamIdsCaptor.getValue()).containsExactly(g2.getId());
    }
}
