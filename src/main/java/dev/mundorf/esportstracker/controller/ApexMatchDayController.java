package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.ApexMapper;
import dev.mundorf.esportstracker.model.dto.ApexMatchDayDetailResponse;
import dev.mundorf.esportstracker.model.dto.ApexMatchDayResponse;
import dev.mundorf.esportstracker.model.dto.ApexTeamResultResponse;
import dev.mundorf.esportstracker.model.dto.PagedResponse;
import dev.mundorf.esportstracker.model.entity.ApexGameResult;
import dev.mundorf.esportstracker.model.entity.ApexMatchDay;
import dev.mundorf.esportstracker.model.entity.ApexTeamResult;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.repository.ApexGameResultRepository;
import dev.mundorf.esportstracker.service.ApexMatchDayService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/apex/matchdays")
public class ApexMatchDayController {

    private final ApexMatchDayService matchDayService;
    private final ApexGameResultRepository gameResultRepository;
    private final ApexMapper apexMapper;

    public ApexMatchDayController(ApexMatchDayService matchDayService,
                                  ApexGameResultRepository gameResultRepository, ApexMapper apexMapper) {
        this.matchDayService = matchDayService;
        this.gameResultRepository = gameResultRepository;
        this.apexMapper = apexMapper;
    }

    @GetMapping
    public PagedResponse<ApexMatchDayResponse> list(
            @RequestParam(required = false) String league,
            @RequestParam(required = false) EventStatus status,
            @PageableDefault(size = 20, sort = "startsAt") Pageable pageable) {
        return PagedResponse.from(matchDayService.search(league, status, pageable), apexMapper::toResponse);
    }

    @GetMapping("/{id}")
    public ApexMatchDayDetailResponse detail(@PathVariable UUID id) {
        ApexMatchDay day = matchDayService.findById(id);
        List<ApexTeamResult> results = matchDayService.findResults(id);

        // One query for every game breakdown of the day, grouped per team result - not one
        // query per team (a 20-team match day would otherwise cost 20 round trips).
        List<UUID> resultIds = results.stream().map(ApexTeamResult::getId).toList();
        Map<UUID, List<ApexGameResult>> gamesByResult = resultIds.isEmpty()
                ? Map.of()
                : gameResultRepository.findByTeamResultIdIn(resultIds).stream()
                        .collect(Collectors.groupingBy(g -> g.getTeamResult().getId()));

        List<ApexTeamResultResponse> resultResponses = results.stream()
                .map(r -> apexMapper.toResultResponse(r, gamesByResult.getOrDefault(r.getId(), List.of())))
                .toList();
        return apexMapper.toDetailResponse(day, resultResponses);
    }
}
