package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.MatchMapper;
import dev.mundorf.esportstracker.mapper.StandingMapper;
import dev.mundorf.esportstracker.mapper.TournamentMapper;
import dev.mundorf.esportstracker.model.dto.MatchResponse;
import dev.mundorf.esportstracker.model.dto.PagedResponse;
import dev.mundorf.esportstracker.model.dto.StandingResponse;
import dev.mundorf.esportstracker.model.dto.TournamentResponse;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.service.MatchService;
import dev.mundorf.esportstracker.service.StandingService;
import dev.mundorf.esportstracker.service.TournamentService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;
    private final MatchService matchService;
    private final StandingService standingService;
    private final TournamentMapper tournamentMapper;
    private final MatchMapper matchMapper;
    private final StandingMapper standingMapper;

    public TournamentController(TournamentService tournamentService, MatchService matchService,
                                StandingService standingService, TournamentMapper tournamentMapper,
                                MatchMapper matchMapper, StandingMapper standingMapper) {
        this.tournamentService = tournamentService;
        this.matchService = matchService;
        this.standingService = standingService;
        this.tournamentMapper = tournamentMapper;
        this.matchMapper = matchMapper;
        this.standingMapper = standingMapper;
    }

    @GetMapping
    public PagedResponse<TournamentResponse> list(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) TournamentTier tier,
            @PageableDefault(size = 20) Pageable pageable) {
        return PagedResponse.from(
                tournamentService.search(game, status, tier, pageable), tournamentMapper::toResponse);
    }

    @GetMapping("/{id}")
    public TournamentResponse get(@PathVariable UUID id) {
        return tournamentMapper.toResponse(tournamentService.findById(id));
    }

    @GetMapping("/{id}/matches")
    public PagedResponse<MatchResponse> matches(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PagedResponse.from(
                matchService.findByTournament(id, pageable), matchMapper::toResponse);
    }

    @GetMapping("/{id}/standings")
    public List<StandingResponse> standings(@PathVariable UUID id) {
        return standingService.findByTournament(id).stream().map(standingMapper::toResponse).toList();
    }
}
