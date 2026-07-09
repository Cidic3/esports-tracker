package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.mapper.MatchMapper;
import dev.mundorf.esportstracker.mapper.TeamMapper;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse;
import dev.mundorf.esportstracker.security.JwtAuthenticationFilter;
import dev.mundorf.esportstracker.service.MatchDetailsService;
import dev.mundorf.esportstracker.service.MatchService;
import dev.mundorf.esportstracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MatchController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import({MatchMapper.class, TeamMapper.class})
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @MockBean
    private MatchDetailsService matchDetailsService;

    @MockBean
    private UserService userService;

    private final Game lolGame = new Game("League of Legends", "league-of-legends", null);
    private final League lec = new League("LEC", "lec", "EMEA", lolGame, "L1");
    private final Tournament tournament = new Tournament("LEC Split 2 2026", "lec_split_2_2026", lec, lolGame,
            LocalDate.now().minusDays(30), LocalDate.now().plusDays(30),
            TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1");

    private Match sampleMatch(String externalId) {
        Team teamA = new Team("G2 Esports", "g2-esports", null, lolGame, "TA");
        Team teamB = new Team("Fnatic", "fnatic", null, lolGame, "TB");
        return new Match(tournament, lolGame, teamA, teamB, Instant.parse("2026-04-06T17:15:00Z"),
                EventStatus.FINISHED, 3, 1, null, externalId);
    }

    @Test
    void shouldListMatchesWithFilters() throws Exception {
        when(matchService.search(eq("league-of-legends"), eq(EventStatus.FINISHED), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleMatch("M1")), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/matches")
                        .param("game", "league-of-legends")
                        .param("status", "FINISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].scoreA").value(3))
                .andExpect(jsonPath("$.content[0].scoreB").value(1));
    }

    @Test
    void shouldConvertDateOnlyFiltersToFullUtcDayBoundaries() throws Exception {
        when(matchService.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/matches").param("from", "2026-04-06").param("to", "2026-04-06"))
                .andExpect(status().isOk());

        verify(matchService).search(any(), any(), any(),
                eq(Instant.parse("2026-04-06T00:00:00Z")),
                eq(Instant.parse("2026-04-06T23:59:59.999999999Z")),
                any());
    }

    @Test
    void shouldReturnTodaysMatches() throws Exception {
        when(matchService.findToday()).thenReturn(List.of(sampleMatch("M2")));

        mockMvc.perform(get("/api/matches/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tournamentName").value("LEC Split 2 2026"));
    }

    @Test
    void shouldGetMatchById() throws Exception {
        UUID id = UUID.randomUUID();
        when(matchService.findById(id)).thenReturn(sampleMatch("M3"));

        mockMvc.perform(get("/api/matches/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamA.name").value("G2 Esports"));
    }

    @Test
    void shouldReturn404WhenMatchNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(matchService.findById(id)).thenThrow(new ResourceNotFoundException("Match not found: " + id));

        mockMvc.perform(get("/api/matches/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnMatchDetailsWithPerGameStats() throws Exception {
        UUID id = UUID.randomUUID();
        var player = new MatchDetailsResponse.PlayerGameDetails(
                "EWI Vizicsacsi", "Rumble", "top", 16, 8, 2, 7, 198, 12483L, List.of(3009, 6653));
        var blue = new MatchDetailsResponse.TeamGameDetails(
                UUID.randomUUID(), "G2 Esports", 25, 58000, 9, 2, List.of("ocean", "infernal"), List.of(player));
        var game = new MatchDetailsResponse.GameDetails(1, "completed", blue, null);
        when(matchDetailsService.getDetails(id)).thenReturn(new MatchDetailsResponse(id, List.of(game)));

        mockMvc.perform(get("/api/matches/{id}/details", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games[0].number").value(1))
                .andExpect(jsonPath("$.games[0].blueTeam.players[0].champion").value("Rumble"))
                .andExpect(jsonPath("$.games[0].blueTeam.players[0].items[0]").value(3009))
                .andExpect(jsonPath("$.games[0].blueTeam.dragons[1]").value("infernal"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnUpcomingMatchesForUser() throws Exception {
        User user = new User("testuser", "test@example.com", "hashed-password");
        when(userService.findByUsername("testuser")).thenReturn(user);
        when(matchService.findUpcomingForUser(eq(user), any()))
                .thenReturn(new PageImpl<>(List.of(sampleMatch("M4")), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/matches/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].teamA.name").value("G2 Esports"));
    }
}
