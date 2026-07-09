package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.mapper.MatchMapper;
import dev.mundorf.esportstracker.mapper.StandingMapper;
import dev.mundorf.esportstracker.mapper.TeamMapper;
import dev.mundorf.esportstracker.mapper.LeagueMapper;
import dev.mundorf.esportstracker.mapper.TournamentMapper;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Standing;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.security.JwtAuthenticationFilter;
import dev.mundorf.esportstracker.service.MatchService;
import dev.mundorf.esportstracker.service.StandingService;
import dev.mundorf.esportstracker.service.TournamentService;
import org.junit.jupiter.api.Test;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TournamentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import({TournamentMapper.class, LeagueMapper.class, MatchMapper.class, TeamMapper.class, StandingMapper.class})
class TournamentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TournamentService tournamentService;
    @MockBean
    private MatchService matchService;
    @MockBean
    private StandingService standingService;

    private final Game lolGame = new Game("League of Legends", "league-of-legends", null);
    private final League lec = new League("LEC", "lec", "EMEA", lolGame, "L1");

    @Test
    void shouldListTournamentsWithFilters() throws Exception {
        Tournament worlds = new Tournament("Worlds 2026", "worlds_2026", lec, lolGame,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 20),
                TournamentTier.INTERNATIONAL, EventStatus.UPCOMING, null, "T1");
        when(tournamentService.search(eq("league-of-legends"), eq(EventStatus.UPCOMING),
                isNull(), any())).thenReturn(new PageImpl<>(List.of(worlds), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/tournaments")
                        .param("game", "league-of-legends")
                        .param("status", "UPCOMING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Worlds 2026"))
                .andExpect(jsonPath("$.content[0].tier").value("INTERNATIONAL"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldGetTournamentById() throws Exception {
        UUID id = UUID.randomUUID();
        Tournament tournament = new Tournament("LEC Split 3 2026", "lec_split_3_2026", lec, lolGame,
                LocalDate.of(2026, 7, 23), LocalDate.of(2026, 9, 20),
                TournamentTier.PRIMARY, EventStatus.RUNNING, new BigDecimal("100000.00"), "T2");
        when(tournamentService.findById(id)).thenReturn(tournament);

        mockMvc.perform(get("/api/tournaments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("LEC Split 3 2026"))
                .andExpect(jsonPath("$.league.slug").value("lec"))
                .andExpect(jsonPath("$.prizePool").value(100000.00));
    }

    @Test
    void shouldReturn404WhenTournamentNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(tournamentService.findById(id)).thenThrow(new ResourceNotFoundException("Tournament not found: " + id));

        mockMvc.perform(get("/api/tournaments/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListMatchesForTournament() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = new Tournament("LEC Split 2 2026", "lec_split_2_2026", lec, lolGame,
                LocalDate.now().minusDays(30), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T3");
        Team teamA = new Team("G2 Esports", "g2-esports", null, lolGame, "TA");
        Team teamB = new Team("Fnatic", "fnatic", null, lolGame, "TB");
        Match match = new Match(tournament, lolGame, teamA, teamB, Instant.parse("2026-04-06T17:15:00Z"),
                EventStatus.FINISHED, 3, 1, null, "M1");
        when(matchService.findByTournament(eq(tournamentId), any()))
                .thenReturn(new PageImpl<>(List.of(match), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/tournaments/{id}/matches", tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].teamA.name").value("G2 Esports"))
                .andExpect(jsonPath("$.content[0].teamB.name").value("Fnatic"))
                .andExpect(jsonPath("$.content[0].scoreA").value(3));
    }

    @Test
    void shouldListStandingsForTournament() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = new Tournament("LEC Split 2 2026", "lec_split_2_2026", lec, lolGame,
                LocalDate.now().minusDays(30), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T4");
        Team team = new Team("G2 Esports", "g2-esports", null, lolGame, "TA");
        Standing standing = new Standing(tournament, team, "Regular Season", 1, 8, 1);
        when(standingService.findByTournament(tournamentId)).thenReturn(List.of(standing));

        mockMvc.perform(get("/api/tournaments/{id}/standings", tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupName").value("Regular Season"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].wins").value(8))
                .andExpect(jsonPath("$[0].losses").value(1))
                .andExpect(jsonPath("$[0].team.name").value("G2 Esports"));
    }
}
