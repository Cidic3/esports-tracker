package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.MatchMapper;
import dev.mundorf.esportstracker.model.dto.MatchResponse;
import dev.mundorf.esportstracker.model.dto.PagedResponse;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.service.MatchService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final MatchMapper matchMapper;

    public MatchController(MatchService matchService, MatchMapper matchMapper) {
        this.matchService = matchService;
        this.matchMapper = matchMapper;
    }

    @GetMapping
    public PagedResponse<MatchResponse> list(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) UUID team,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // date-only filters map to the full UTC day: from = 00:00:00, to = 23:59:59.999999999
        Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to == null ? null : to.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        return PagedResponse.from(
                matchService.search(game, status, team, fromInstant, toInstant, pageable),
                matchMapper::toResponse);
    }

    @GetMapping("/today")
    public List<MatchResponse> today() {
        return matchService.findToday().stream().map(matchMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MatchResponse get(@PathVariable UUID id) {
        return matchMapper.toResponse(matchService.findById(id));
    }
}
