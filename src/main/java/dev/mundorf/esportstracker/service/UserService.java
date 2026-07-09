package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.GameRepository;
import dev.mundorf.esportstracker.repository.LeagueRepository;
import dev.mundorf.esportstracker.repository.TeamRepository;
import dev.mundorf.esportstracker.repository.UserRepository;
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

    public UserService(UserRepository userRepository, GameRepository gameRepository,
                       TeamRepository teamRepository, LeagueRepository leagueRepository) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
        this.leagueRepository = leagueRepository;
    }

    public User findByUsername(String username) {
        return userRepository.findWithFollowsByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional
    public User updateFollowedGames(String username, List<String> slugs) {
        User user = findByUsername(username);
        Set<String> requestedSlugs = new HashSet<>(slugs);
        List<Game> games = gameRepository.findBySlugIn(slugs);
        if (games.size() != requestedSlugs.size()) {
            throw new ResourceNotFoundException("One or more game slugs not found");
        }
        user.replaceFollowedGames(new HashSet<>(games));
        return user;
    }

    @Transactional
    public User updateFollowedTeams(String username, List<UUID> teamIds) {
        User user = findByUsername(username);
        Set<UUID> requestedIds = new HashSet<>(teamIds);
        List<Team> teams = teamRepository.findAllById(teamIds);
        if (teams.size() != requestedIds.size()) {
            throw new ResourceNotFoundException("One or more team ids not found");
        }
        user.replaceFollowedTeams(new HashSet<>(teams));
        return user;
    }

    @Transactional
    public User updateFollowedLeagues(String username, List<UUID> leagueIds) {
        User user = findByUsername(username);
        Set<UUID> requestedIds = new HashSet<>(leagueIds);
        List<League> leagues = leagueRepository.findAllById(leagueIds);
        if (leagues.size() != requestedIds.size()) {
            throw new ResourceNotFoundException("One or more league ids not found");
        }
        user.replaceFollowedLeagues(new HashSet<>(leagues));
        return user;
    }
}
