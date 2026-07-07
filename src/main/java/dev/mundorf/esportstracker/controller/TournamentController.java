package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.MatchMapper;
import dev.mundorf.esportstracker.mapper.TournamentMapper;
import dev.mundorf.esportstracker.model.dto.MatchResponse;
import dev.mundorf.esportstracker.model.dto.PagedResponse;
import dev.mundorf.esportstracker.model.dto.TournamentResponse;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.service.MatchService;
import dev.mundorf.esportstracker.service.TournamentService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;
    private final MatchService matchService;
    private final TournamentMapper tournamentMapper;
    private final MatchMapper matchMapper;

    public TournamentController(TournamentService tournamentService, MatchService matchService,
                                TournamentMapper tournamentMapper, MatchMapper matchMapper) {
        this.tournamentService = tournamentService;
        this.matchService = matchService;
        this.tournamentMapper = tournamentMapper;
        this.matchMapper = matchMapper;
    }

    @GetMapping
    public PagedResponse<TournamentResponse> list(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) TournamentTier tier,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
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
}
