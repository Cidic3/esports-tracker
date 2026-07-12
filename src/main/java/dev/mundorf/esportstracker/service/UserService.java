package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.exception.StaleUpdateException;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.GameRepository;
import dev.mundorf.esportstracker.repository.LeagueRepository;
import dev.mundorf.esportstracker.repository.TeamRepository;
import dev.mundorf.esportstracker.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;
    private final EntityManager entityManager;

    public UserService(UserRepository userRepository, GameRepository gameRepository,
                       TeamRepository teamRepository, LeagueRepository leagueRepository,
                       EntityManager entityManager) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
        this.leagueRepository = leagueRepository;
        this.entityManager = entityManager;
    }

    public User findByUsername(String username) {
        return userRepository.findWithFollowsByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional
    public User updateFollowedGames(String username, List<String> slugs, long expectedVersion) {
        User user = findByUsername(username);
        checkVersion(user, expectedVersion);
        Set<String> requestedSlugs = new HashSet<>(slugs);
        List<Game> games = gameRepository.findBySlugIn(slugs);
        if (games.size() != requestedSlugs.size()) {
            throw new ResourceNotFoundException("One or more game slugs not found");
        }
        user.replaceFollowedGames(new HashSet<>(games));
        bumpVersion(user);
        return user;
    }

    @Transactional
    public User updateFollowedTeams(String username, List<UUID> teamIds, long expectedVersion) {
        User user = findByUsername(username);
        checkVersion(user, expectedVersion);
        Set<UUID> requestedIds = new HashSet<>(teamIds);
        List<Team> teams = teamRepository.findAllById(teamIds);
        if (teams.size() != requestedIds.size()) {
            throw new ResourceNotFoundException("One or more team ids not found");
        }
        user.replaceFollowedTeams(new HashSet<>(teams));
        bumpVersion(user);
        return user;
    }

    @Transactional
    public User updateFollowedLeagues(String username, List<UUID> leagueIds, long expectedVersion) {
        User user = findByUsername(username);
        checkVersion(user, expectedVersion);
        Set<UUID> requestedIds = new HashSet<>(leagueIds);
        List<League> leagues = leagueRepository.findAllById(leagueIds);
        if (leagues.size() != requestedIds.size()) {
            throw new ResourceNotFoundException("One or more league ids not found");
        }
        user.replaceFollowedLeagues(new HashSet<>(leagues));
        bumpVersion(user);
        return user;
    }

    /**
     * Rejects a follow-update PUT computed from a profile the client read before someone else's
     * change landed - e.g. two browser tabs, or a second toggle fired before the first one's
     * response updated the UI's cached follow list. Without this, the second full-replace write
     * would silently overwrite the first (last write wins, no error) since each PUT reloads the
     * user fresh and has no memory of what the client last saw. See User.version.
     */
    private void checkVersion(User user, long expectedVersion) {
        if (user.getVersion() != expectedVersion) {
            throw new StaleUpdateException(
                    "Your follows changed elsewhere - refresh and try again");
        }
    }

    /**
     * Hibernate's automatic @Version increment only fires when the owning entity's own row is
     * dirtied - a pure @ManyToMany join-table change (add/remove a follow) never touches the
     * `users` row itself, so the version would otherwise silently stay put forever and checkVersion
     * could never detect a race (confirmed by direct inspection: version and updated_at both stayed
     * unchanged after a real follow update, in this Hibernate version, before this fix). Explicitly
     * requesting OPTIMISTIC_FORCE_INCREMENT tells Hibernate to bump the version and issue the
     * incrementing UPDATE regardless of whether any scalar column changed.
     */
    private void bumpVersion(User user) {
        entityManager.lock(user, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }
}
