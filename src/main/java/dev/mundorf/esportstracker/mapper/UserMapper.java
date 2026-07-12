package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.UserResponse;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class UserMapper {

    private final GameMapper gameMapper;
    private final LeagueMapper leagueMapper;
    private final TeamMapper teamMapper;

    public UserMapper(GameMapper gameMapper, LeagueMapper leagueMapper, TeamMapper teamMapper) {
        this.gameMapper = gameMapper;
        this.leagueMapper = leagueMapper;
        this.teamMapper = teamMapper;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getFollowedGames().stream()
                        .sorted(Comparator.comparing(Game::getName))
                        .map(gameMapper::toResponse)
                        .toList(),
                user.getFollowedLeagues().stream()
                        .sorted(Comparator.comparing(League::getName))
                        .map(leagueMapper::toResponse)
                        .toList(),
                user.getFollowedTeams().stream()
                        .sorted(Comparator.comparing(Team::getName))
                        .map(teamMapper::toResponse)
                        .toList(),
                user.getVersion());
    }
}
