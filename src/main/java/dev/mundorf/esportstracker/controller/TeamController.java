package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.MatchMapper;
import dev.mundorf.esportstracker.mapper.PlayerMapper;
import dev.mundorf.esportstracker.mapper.StandingMapper;
import dev.mundorf.esportstracker.mapper.TeamMapper;
import dev.mundorf.esportstracker.model.dto.PagedResponse;
import dev.mundorf.esportstracker.model.dto.TeamDetailResponse;
import dev.mundorf.esportstracker.model.dto.TeamSummaryResponse;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Player;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.service.StandingService;
import dev.mundorf.esportstracker.service.TeamService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final StandingService standingService;
    private final TeamMapper teamMapper;
    private final PlayerMapper playerMapper;
    private final StandingMapper standingMapper;
    private final MatchMapper matchMapper;

    public TeamController(TeamService teamService, StandingService standingService, TeamMapper teamMapper,
                          PlayerMapper playerMapper, StandingMapper standingMapper, MatchMapper matchMapper) {
        this.teamService = teamService;
        this.standingService = standingService;
        this.teamMapper = teamMapper;
        this.playerMapper = playerMapper;
        this.standingMapper = standingMapper;
        this.matchMapper = matchMapper;
    }

    @GetMapping
    public PagedResponse<TeamSummaryResponse> list(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return PagedResponse.from(teamService.search(game, search, pageable), teamMapper::toSummaryResponse);
    }

    @GetMapping("/{id}")
    public TeamDetailResponse detail(@PathVariable UUID id) {
        Team team = teamService.findById(id);
        List<Match> recentMatches = teamService.findRecentMatches(id);
        teamService.warmMatchDetails(recentMatches);
        Set<String> activeSummonerNames = teamService.findActiveSummonerNames(id, recentMatches);

        List<Player> roster = teamService.findRoster(id);
        return teamMapper.toDetailResponse(
                team,
                roster.stream()
                        .map(p -> playerMapper.toResponse(
                                p, TeamService.isActive(activeSummonerNames, p.getSummonerName())))
                        .toList(),
                standingService.findByTeam(id).stream().map(standingMapper::toResponse).toList(),
                teamService.findLiveMatches(id).stream().map(matchMapper::toResponse).toList(),
                recentMatches.stream().map(matchMapper::toResponse).toList(),
                teamService.findUpcomingMatches(id).stream().map(matchMapper::toResponse).toList());
    }
}
