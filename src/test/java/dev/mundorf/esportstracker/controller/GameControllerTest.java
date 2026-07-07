package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.mapper.GameMapper;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.security.JwtAuthenticationFilter;
import dev.mundorf.esportstracker.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest boots only the MVC layer around GameController (no database, no full app context).
 * This controller is entirely public (GET /api/games/**), so there's no auth-dependent behavior to
 * verify. JwtAuthenticationFilter is excluded from component scanning: as a @Component implementing
 * Filter it would otherwise be auto-detected and constructed by this slice regardless of
 * addFilters, pulling in its real dependencies (JwtService, CustomUserDetailsService ->
 * UserRepository) that this test has no way to provide. GameMapper is imported for real (pure,
 * dependency-free) so the test also verifies the actual JSON shape, not just that a mock was called.
 */
@WebMvcTest(
        controllers = GameController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GameMapper.class)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @Test
    void shouldListGames() throws Exception {
        Game lol = new Game("League of Legends", "league-of-legends", "icon.png");
        when(gameService.findAll()).thenReturn(List.of(lol));

        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("league-of-legends"))
                .andExpect(jsonPath("$[0].name").value("League of Legends"));
    }

    @Test
    void shouldGetGameBySlug() throws Exception {
        Game lol = new Game("League of Legends", "league-of-legends", null);
        when(gameService.findBySlug("league-of-legends")).thenReturn(lol);

        mockMvc.perform(get("/api/games/league-of-legends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("League of Legends"));
    }

    @Test
    void shouldReturn404WhenGameNotFound() throws Exception {
        when(gameService.findBySlug("unknown"))
                .thenThrow(new ResourceNotFoundException("Game not found: unknown"));

        mockMvc.perform(get("/api/games/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Game not found: unknown"));
    }
}
