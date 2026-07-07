package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public List<Game> findAll() {
        return gameRepository.findAll();
    }

    public Game findBySlug(String slug) {
        return gameRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + slug));
    }
}
