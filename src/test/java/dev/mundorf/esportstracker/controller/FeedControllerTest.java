package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.mapper.MatchMapper;
import dev.mundorf.esportstracker.mapper.TeamMapper;
import dev.mundorf.esportstracker.mapper.TournamentMapper;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.security.JwtAuthenticationFilter;
import dev.mundorf.esportstracker.service.MatchService;
import dev.mundorf.esportstracker.service.TournamentService;
import dev.mundorf.esportstracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Keeps the security filter chain enabled (like UserControllerTest) since /api/feed depends on
 * @AuthenticationPrincipal - see the gotcha documented in UserControllerTest.
 */
@WebMvcTest(
        controllers = FeedController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import({MatchMapper.class, TournamentMapper.class, TeamMapper.class})
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;
    @MockBean
    private TournamentService tournamentService;
    @MockBean
    private UserService userService;

    private final Game lolGame = new Game("League of Legends", "league-of-legends", null);
    private final League lec = new League("LEC", "lec", "EMEA", lolGame, "L1");

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnUpcomingMatchesAndRunningTournaments() throws Exception {
        User user = new User("testuser", "test@example.com", "hashed-password");
        when(userService.findByUsername("testuser")).thenReturn(user);

        Tournament tournament = new Tournament("LEC Split 2 2026", "lec_split_2_2026", lec, lolGame,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(20),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1");
        Team teamA = new Team("G2 Esports", "g2-esports", null, lolGame, "TA");
        Team teamB = new Team("Fnatic", "fnatic", null, lolGame, "TB");
        Match match = new Match(tournament, lolGame, teamA, teamB, Instant.parse("2026-08-01T17:00:00Z"),
                EventStatus.UPCOMING, null, null, null, "M1");

        when(matchService.findUpcomingForUser(eq(user), any()))
                .thenReturn(new PageImpl<>(List.of(match), PageRequest.of(0, 20), 1));
        when(tournamentService.findRunningForUser(eq(user), eq(20)))
                .thenReturn(List.of(tournament));

        mockMvc.perform(get("/api/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingMatches[0].teamA.name").value("G2 Esports"))
                .andExpect(jsonPath("$.runningTournaments[0].name").value("LEC Split 2 2026"));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/feed"))
                .andExpect(status().is4xxClientError());
    }
}
