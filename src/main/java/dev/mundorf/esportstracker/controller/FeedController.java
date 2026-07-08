package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.MatchMapper;
import dev.mundorf.esportstracker.mapper.TournamentMapper;
import dev.mundorf.esportstracker.model.dto.FeedResponse;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.service.MatchService;
import dev.mundorf.esportstracker.service.TournamentService;
import dev.mundorf.esportstracker.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
@SecurityRequirement(name = "bearer-jwt")
public class FeedController {

    // Dashboard glance, not a full listing - /api/matches/upcoming and /api/tournaments?status=RUNNING
    // already provide paginated access to the full sets.
    private static final int FEED_LIMIT = 20;

    private final MatchService matchService;
    private final TournamentService tournamentService;
    private final UserService userService;
    private final MatchMapper matchMapper;
    private final TournamentMapper tournamentMapper;

    public FeedController(MatchService matchService, TournamentService tournamentService, UserService userService,
                          MatchMapper matchMapper, TournamentMapper tournamentMapper) {
        this.matchService = matchService;
        this.tournamentService = tournamentService;
        this.userService = userService;
        this.matchMapper = matchMapper;
        this.tournamentMapper = tournamentMapper;
    }

    @GetMapping
    public FeedResponse getFeed(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        Pageable pageable = PageRequest.of(0, FEED_LIMIT, Sort.by(Sort.Direction.ASC, "scheduledAt"));

        var upcomingMatches = matchService.findUpcomingForUser(user, pageable)
                .map(matchMapper::toResponse)
                .getContent();
        var runningTournaments = tournamentService.findRunningForUser(user, FEED_LIMIT).stream()
                .map(tournamentMapper::toResponse)
                .toList();

        return new FeedResponse(upcomingMatches, runningTournaments);
    }
}
