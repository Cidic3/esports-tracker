package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.mapper.ApexMapper;
import dev.mundorf.esportstracker.mapper.MatchMapper;
import dev.mundorf.esportstracker.mapper.PlayerMapper;
import dev.mundorf.esportstracker.mapper.StandingMapper;
import dev.mundorf.esportstracker.mapper.TeamMapper;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.Player;
import dev.mundorf.esportstracker.model.entity.PlayerRole;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.security.JwtAuthenticationFilter;
import dev.mundorf.esportstracker.service.ApexMatchDayService;
import dev.mundorf.esportstracker.service.StandingService;
import dev.mundorf.esportstracker.service.TeamService;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TeamController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import({TeamMapper.class, PlayerMapper.class, StandingMapper.class, MatchMapper.class, ApexMapper.class})
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamService teamService;
    @MockBean
    private StandingService standingService;
    @MockBean
    private ApexMatchDayService apexMatchDayService;

    private final Game lolGame = new Game("League of Legends", "league-of-legends", null);

    @Test
    void shouldListTeamsWithFilters() throws Exception {
        Team g2 = new Team("G2 Esports", "g2-esports", "logo.png", lolGame, "TA");
        when(teamService.search(eq("league-of-legends"), eq("g2"), any()))
                .thenReturn(new PageImpl<>(List.of(g2), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/teams")
                        .param("game", "league-of-legends")
                        .param("search", "g2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("G2 Esports"))
                .andExpect(jsonPath("$.content[0].gameSlug").value("league-of-legends"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnTeamDetailWithRosterStandingsAndMatches() throws Exception {
        UUID teamId = UUID.randomUUID();
        Team team = new Team("G2 Esports", "g2-esports", "logo.png", lolGame, "TA");
        Player top = new Player(team, "BrokenBlade", "Sergen", "Celik", null, PlayerRole.TOP, "P1");
        Player bench = new Player(team, "Benchwarmer", null, null, null, PlayerRole.NONE, "P2");

        when(teamService.findById(teamId)).thenReturn(team);
        when(teamService.findRecentMatches(teamId)).thenReturn(List.of());
        when(teamService.findActiveSummonerNames(eq(teamId), any())).thenReturn(Set.of("brokenblade"));
        when(teamService.findRoster(teamId)).thenReturn(List.of(top, bench));
        when(teamService.findLiveMatches(teamId)).thenReturn(List.of());
        when(teamService.findUpcomingMatches(teamId)).thenReturn(List.of());
        when(standingService.findByTeam(teamId)).thenReturn(List.of());

        mockMvc.perform(get("/api/teams/{id}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("G2 Esports"))
                .andExpect(jsonPath("$.roster[0].summonerName").value("BrokenBlade"))
                .andExpect(jsonPath("$.roster[0].active").value(true))
                .andExpect(jsonPath("$.roster[1].summonerName").value("Benchwarmer"))
                .andExpect(jsonPath("$.roster[1].active").value(false));
    }

    @Test
    void shouldReturn404WhenTeamNotFound() throws Exception {
        UUID teamId = UUID.randomUUID();
        when(teamService.findById(teamId)).thenThrow(new ResourceNotFoundException("Team not found: " + teamId));

        mockMvc.perform(get("/api/teams/{id}", teamId))
                .andExpect(status().isNotFound());
    }
}
