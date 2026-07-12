package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.UserMapper;
import dev.mundorf.esportstracker.model.dto.FollowGamesRequest;
import dev.mundorf.esportstracker.model.dto.FollowLeaguesRequest;
import dev.mundorf.esportstracker.model.dto.FollowTeamsRequest;
import dev.mundorf.esportstracker.model.dto.UserResponse;
import dev.mundorf.esportstracker.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userMapper.toResponse(userService.findByUsername(userDetails.getUsername()));
    }

    @PutMapping("/me/games")
    public UserResponse updateFollowedGames(@AuthenticationPrincipal UserDetails userDetails,
                                            @Valid @RequestBody FollowGamesRequest request) {
        return userMapper.toResponse(userService.updateFollowedGames(
                userDetails.getUsername(), request.slugs(), request.version()));
    }

    @PutMapping("/me/teams")
    public UserResponse updateFollowedTeams(@AuthenticationPrincipal UserDetails userDetails,
                                            @Valid @RequestBody FollowTeamsRequest request) {
        return userMapper.toResponse(userService.updateFollowedTeams(
                userDetails.getUsername(), request.teamIds(), request.version()));
    }

    @PutMapping("/me/leagues")
    public UserResponse updateFollowedLeagues(@AuthenticationPrincipal UserDetails userDetails,
                                              @Valid @RequestBody FollowLeaguesRequest request) {
        return userMapper.toResponse(userService.updateFollowedLeagues(
                userDetails.getUsername(), request.leagueIds(), request.version()));
    }
}
