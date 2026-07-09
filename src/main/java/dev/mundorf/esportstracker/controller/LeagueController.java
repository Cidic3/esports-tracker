package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.LeagueMapper;
import dev.mundorf.esportstracker.model.dto.LeagueResponse;
import dev.mundorf.esportstracker.service.LeagueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leagues")
public class LeagueController {

    private final LeagueService leagueService;
    private final LeagueMapper leagueMapper;

    public LeagueController(LeagueService leagueService, LeagueMapper leagueMapper) {
        this.leagueService = leagueService;
        this.leagueMapper = leagueMapper;
    }

    @GetMapping
    public List<LeagueResponse> list(@RequestParam(required = false) String game) {
        return leagueService.findAll(game).stream().map(leagueMapper::toResponse).toList();
    }
}
