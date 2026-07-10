package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.PlayerResponse;
import dev.mundorf.esportstracker.model.entity.Player;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {

    public PlayerResponse toResponse(Player player, boolean active) {
        return new PlayerResponse(
                player.getId(),
                player.getSummonerName(),
                player.getFirstName(),
                player.getLastName(),
                player.getImageUrl(),
                player.getRole(),
                active);
    }
}
