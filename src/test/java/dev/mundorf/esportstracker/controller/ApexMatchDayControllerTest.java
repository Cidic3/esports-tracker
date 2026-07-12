package dev.mundorf.esportstracker.controller;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.mapper.ApexMapper;
import dev.mundorf.esportstracker.mapper.TeamMapper;
import dev.mundorf.esportstracker.model.entity.ApexGameResult;
import dev.mundorf.esportstracker.model.entity.ApexMatchDay;
import dev.mundorf.esportstracker.model.entity.ApexTeamResult;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.repository.ApexGameResultRepository;
import dev.mundorf.esportstracker.security.JwtAuthenticationFilter;
import dev.mundorf.esportstracker.service.ApexMatchDayService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ApexMatchDayController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApexMapper.class, TeamMapper.class})
class ApexMatchDayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApexMatchDayService apexMatchDayService;
    @MockBean
    private ApexGameResultRepository gameResultRepository;

    private final Game apexGame = new Game("Apex Legends", "apex-legends", null);
    private final League algsEmea = new League("ALGS EMEA", "algs-emea", "EMEA", apexGame, "algs-emea");
    private final Tournament tournament = new Tournament("ALGS Year 6 Split 1 Pro League EMEA",
            "year-6-split-1-pro-league-emea", algsEmea, apexGame,
            LocalDate.of(2026, 4, 4), LocalDate.of(2026, 6, 7),
            TournamentTier.PRIMARY, EventStatus.RUNNING, null, "year-6/split-1-pro-league-emea");

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    @Test
    void shouldListMatchDaysWithFilters() throws Exception {
        ApexMatchDay day = new ApexMatchDay(tournament, apexGame, "Group A vs B",
                Instant.parse("2026-04-04T04:00:00Z"), EventStatus.FINISHED, "E1");
        when(apexMatchDayService.search(eq("algs-emea"), eq(EventStatus.FINISHED), any()))
                .thenReturn(new PageImpl<>(List.of(day), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/apex/matchdays")
                        .param("league", "algs-emea")
                        .param("status", "FINISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Group A vs B"))
                .andExpect(jsonPath("$.content[0].leagueSlug").value("algs-emea"))
                .andExpect(jsonPath("$.content[0].tournamentName").value("ALGS Year 6 Split 1 Pro League EMEA"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnMatchDayDetailWithRankedResultsAndGameBreakdown() throws Exception {
        UUID dayId = UUID.randomUUID();
        ApexMatchDay day = new ApexMatchDay(tournament, apexGame, "Group A vs B",
                Instant.parse("2026-04-04T04:00:00Z"), EventStatus.FINISHED, "E1");
        Team team = new Team("VK GAMING", "vk-gaming", null, apexGame, "vk-gaming");
        ApexTeamResult result = withId(new ApexTeamResult(day, team, 1, 72));
        ApexGameResult game1 = new ApexGameResult(result, 1, 2, 10, 19);
        ApexGameResult game2 = new ApexGameResult(result, 2, 18, 1, 1);

        when(apexMatchDayService.findById(dayId)).thenReturn(day);
        when(apexMatchDayService.findResults(dayId)).thenReturn(List.of(result));
        when(gameResultRepository.findByTeamResultIdIn(anyCollection())).thenReturn(List.of(game2, game1));

        mockMvc.perform(get("/api/apex/matchdays/{id}", dayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Group A vs B"))
                .andExpect(jsonPath("$.results[0].rank").value(1))
                .andExpect(jsonPath("$.results[0].totalPoints").value(72))
                .andExpect(jsonPath("$.results[0].team.name").value("VK GAMING"))
                // Breakdown comes back sorted by game number regardless of repository order
                .andExpect(jsonPath("$.results[0].games[0].gameNumber").value(1))
                .andExpect(jsonPath("$.results[0].games[0].kills").value(10))
                .andExpect(jsonPath("$.results[0].games[1].gameNumber").value(2))
                .andExpect(jsonPath("$.results[0].games[1].placement").value(18));
    }

    @Test
    void shouldReturn404WhenMatchDayNotFound() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(apexMatchDayService.findById(dayId))
                .thenThrow(new ResourceNotFoundException("Apex match day not found: " + dayId));

        mockMvc.perform(get("/api/apex/matchdays/{id}", dayId))
                .andExpect(status().isNotFound());
    }
}
