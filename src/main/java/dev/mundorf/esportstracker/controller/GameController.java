package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.GameMapper;
import dev.mundorf.esportstracker.model.dto.GameResponse;
import dev.mundorf.esportstracker.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final GameMapper gameMapper;

    public GameController(GameService gameService, GameMapper gameMapper) {
        this.gameService = gameService;
        this.gameMapper = gameMapper;
    }

    @GetMapping
    public List<GameResponse> listGames() {
        return gameService.findAll().stream()
                .map(gameMapper::toResponse)
                .toList();
    }

    @GetMapping("/{slug}")
    public GameResponse getGame(@PathVariable String slug) {
        return gameMapper.toResponse(gameService.findBySlug(slug));
    }
}
