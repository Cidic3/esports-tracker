package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private TournamentService tournamentService;

    private final Game lolGame = withId(new Game("League of Legends", "league-of-legends", null));

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    @Test
    void shouldReturnEmptyListWithoutQueryingWhenUserFollowsNothing() {
        User user = new User("testuser", "test@example.com", "hashed-password");

        List<?> result = tournamentService.findRunningForUser(user, 20);

        assertThat(result).isEmpty();
        verify(tournamentRepository, never()).findRunningForFollowed(any(), any(), any());
    }

    @Test
    void shouldQueryByFollowedGameIdAndSubstitutePlaceholderForEmptyTeams() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        user.replaceFollowedGames(Set.of(lolGame));
        when(tournamentRepository.findRunningForFollowed(any(), any(), any())).thenReturn(List.of());

        tournamentService.findRunningForUser(user, 20);

        ArgumentCaptor<Set<UUID>> gameIdsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set<UUID>> teamIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(tournamentRepository).findRunningForFollowed(gameIdsCaptor.capture(), teamIdsCaptor.capture(), any(Pageable.class));

        assertThat(gameIdsCaptor.getValue()).containsExactly(lolGame.getId());
        assertThat(teamIdsCaptor.getValue()).hasSize(1); // placeholder, since the user follows no teams
    }

    @Test
    void shouldQueryByFollowedTeamId() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        Team g2 = withId(new Team("G2 Esports", "g2-esports", null, lolGame, "TA"));
        user.replaceFollowedTeams(Set.of(g2));
        when(tournamentRepository.findRunningForFollowed(any(), any(), any())).thenReturn(List.of());

        tournamentService.findRunningForUser(user, 20);

        ArgumentCaptor<Set<UUID>> teamIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(tournamentRepository).findRunningForFollowed(any(), teamIdsCaptor.capture(), any(Pageable.class));
        assertThat(teamIdsCaptor.getValue()).containsExactly(g2.getId());
    }
}
