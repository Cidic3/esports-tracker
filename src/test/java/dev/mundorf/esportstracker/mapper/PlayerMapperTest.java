package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.PlayerResponse;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.Player;
import dev.mundorf.esportstracker.model.entity.PlayerRole;
import dev.mundorf.esportstracker.model.entity.Team;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerMapperTest {

    private final PlayerMapper mapper = new PlayerMapper();

    @Test
    void shouldMapPlayerFieldsAndPassThroughActiveFlag() {
        Game lolGame = new Game("League of Legends", "league-of-legends", null);
        Team team = new Team("G2 Esports", "g2-esports", null, lolGame, "TA");
        Player player = new Player(team, "BrokenBlade", "Sergen", "Celik", "img.png", PlayerRole.TOP, "P1");

        PlayerResponse active = mapper.toResponse(player, true);
        PlayerResponse benched = mapper.toResponse(player, false);

        assertThat(active.summonerName()).isEqualTo("BrokenBlade");
        assertThat(active.firstName()).isEqualTo("Sergen");
        assertThat(active.lastName()).isEqualTo("Celik");
        assertThat(active.imageUrl()).isEqualTo("img.png");
        assertThat(active.role()).isEqualTo(PlayerRole.TOP);
        assertThat(active.active()).isTrue();
        assertThat(benched.active()).isFalse();
    }
}
