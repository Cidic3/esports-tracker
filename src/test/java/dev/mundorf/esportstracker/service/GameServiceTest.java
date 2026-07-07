package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    @Test
    void shouldReturnAllGames() {
        Game lol = new Game("League of Legends", "league-of-legends", null);
        Game dota = new Game("Dota 2", "dota-2", null);
        when(gameRepository.findAll()).thenReturn(List.of(lol, dota));

        List<Game> result = gameService.findAll();

        assertThat(result).containsExactly(lol, dota);
    }

    @Test
    void shouldReturnGameBySlug() {
        Game lol = new Game("League of Legends", "league-of-legends", null);
        when(gameRepository.findBySlug("league-of-legends")).thenReturn(Optional.of(lol));

        Game result = gameService.findBySlug("league-of-legends");

        assertThat(result).isEqualTo(lol);
    }

    @Test
    void shouldThrowWhenGameNotFoundBySlug() {
        when(gameRepository.findBySlug("unknown-game")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.findBySlug("unknown-game"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown-game");
    }
}
