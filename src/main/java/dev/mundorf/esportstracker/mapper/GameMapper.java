package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.GameResponse;
import dev.mundorf.esportstracker.model.entity.Game;
import org.springframework.stereotype.Component;

@Component
public class GameMapper {

    public GameResponse toResponse(Game game) {
        return new GameResponse(game.getId(), game.getName(), game.getSlug(), game.getIconUrl());
    }
}
