package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.exception.StaleUpdateException;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.GameRepository;
import dev.mundorf.esportstracker.repository.TeamRepository;
import dev.mundorf.esportstracker.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private UserService userService;

    private final Game lolGame = new Game("League of Legends", "league-of-legends", null);
    private final Game dotaGame = new Game("Dota 2", "dota-2", null);

    /** Entities constructed directly (not persisted) never get Hibernate's @Version default. */
    private static User withVersion(User user, long version) {
        ReflectionTestUtils.setField(user, "version", version);
        return user;
    }

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
        User user = withVersion(new User("testuser", "test@example.com", "hashed-password"), 0L);
        when(userRepository.findWithFollowsByUsername("testuser")).thenReturn(Optional.of(user));
        when(gameRepository.findBySlugIn(List.of("league-of-legends", "dota-2")))
                .thenReturn(List.of(lolGame, dotaGame));

        User result = userService.updateFollowedGames("testuser", List.of("league-of-legends", "dota-2"), 0L);

        assertThat(result.getFollowedGames()).containsExactlyInAnyOrder(lolGame, dotaGame);
        // Collection-only changes never dirty the users row on their own (confirmed by direct DB
        // inspection - see UserService.bumpVersion), so a successful update must explicitly force
        // the version increment rather than relying on Hibernate's automatic dirty-checking.
        verify(entityManager).lock(user, jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    @Test
    void shouldThrowWhenFollowedGameSlugUnknown() {
        User user = withVersion(new User("testuser", "test@example.com", "hashed-password"), 0L);
        when(userRepository.findWithFollowsByUsername("testuser")).thenReturn(Optional.of(user));
        when(gameRepository.findBySlugIn(List.of("league-of-legends", "no-such-game")))
                .thenReturn(List.of(lolGame));

        assertThatThrownBy(() -> userService.updateFollowedGames(
                "testuser", List.of("league-of-legends", "no-such-game"), 0L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReplaceFollowedTeams() {
        User user = withVersion(new User("testuser", "test@example.com", "hashed-password"), 0L);
        Team g2 = new Team("G2 Esports", "g2-esports", null, lolGame, "TA");
        when(userRepository.findWithFollowsByUsername("testuser")).thenReturn(Optional.of(user));
        when(teamRepository.findAllById(List.of(UUID.fromString("00000000-0000-0000-0000-000000000001"))))
                .thenReturn(List.of(g2));

        User result = userService.updateFollowedTeams("testuser",
                List.of(UUID.fromString("00000000-0000-0000-0000-000000000001")), 0L);

        assertThat(result.getFollowedTeams()).containsExactly(g2);
    }

    @Test
    void shouldThrowWhenFollowedTeamIdUnknown() {
        User user = withVersion(new User("testuser", "test@example.com", "hashed-password"), 0L);
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(userRepository.findWithFollowsByUsername("testuser")).thenReturn(Optional.of(user));
        when(teamRepository.findAllById(List.of(unknownId))).thenReturn(List.of());

        assertThatThrownBy(() -> userService.updateFollowedTeams("testuser", List.of(unknownId), 0L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowStaleUpdateWhenSubmittedVersionDoesNotMatch() {
        User user = withVersion(new User("testuser", "test@example.com", "hashed-password"), 1L);
        when(userRepository.findWithFollowsByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateFollowedGames("testuser", List.of("league-of-legends"), 0L))
                .isInstanceOf(StaleUpdateException.class);
    }
}
