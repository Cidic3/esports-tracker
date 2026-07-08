package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.GameRepository;
import dev.mundorf.esportstracker.repository.TeamRepository;
import dev.mundorf.esportstracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private UserService userService;

    private final Game lolGame = new Game("League of Legends", "league-of-legends", null);
    private final Game dotaGame = new Game("Dota 2", "dota-2", null);

    @Test
    void shouldReturnUserByUsername() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        when(userRepository.findWithFollowsByUsername("testuser")).thenReturn(Optional.of(user));

        User result = userService.findByUsername("testuser");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findWithFollowsByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void shouldReplaceFollowedGames() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        when(userRepository.findWithFollowsByUsername("testuser")).thenReturn(Optional.of(user));
        when(gameRepository.findBySlugIn(List.of("league-of-legends", "dota-2")))
                .thenReturn(List.of(lolGame, dotaGame));

        User result = userService.updateFollowedGames("testuser", List.of("league-of-legends", "dota-2"));

        assertThat(result.getFollowedGames()).containsExactlyInAnyOrder(lolGame, dotaGame);
    }

    @Test
    void shouldThrowWhenFollowedGameSlugUnknown() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        when(userRepository.findWithFollowsByUsername("testuser")).thenReturn(Optional.of(user));
        when(gameRepository.findBySlugIn(List.of("league-of-legends", "no-such-game")))
                .thenReturn(List.of(lolGame));

        assertThatThrownBy(() -> userService.updateFollowedGames("testuser", List.of("league-of-legends", "no-such-game")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReplaceFollowedTeams() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        Team g2 = new Team("G2 Esports", "g2-esports", null, lolGame, "TA");
        when(userRepository.findWithFollowsByUsername("testuser")).thenReturn(Optional.of(user));
        when(teamRepository.findAllById(List.of(UUID.fromString("00000000-0000-0000-0000-000000000001"))))
                .thenReturn(List.of(g2));

        User result = userService.updateFollowedTeams("testuser",
                List.of(UUID.fromString("00000000-0000-0000-0000-000000000001")));

        assertThat(result.getFollowedTeams()).containsExactly(g2);
    }

    @Test
    void shouldThrowWhenFollowedTeamIdUnknown() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(userRepository.findWithFollowsByUsername("testuser")).thenReturn(Optional.of(user));
        when(teamRepository.findAllById(List.of(unknownId))).thenReturn(List.of());

        assertThatThrownBy(() -> userService.updateFollowedTeams("testuser", List.of(unknownId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
