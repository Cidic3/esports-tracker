package dev.mundorf.esportstracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mundorf.esportstracker.client.riot.RiotEsportsClient;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.repository.GameRepository;
import dev.mundorf.esportstracker.repository.LeagueRepository;
import dev.mundorf.esportstracker.repository.MatchRepository;
import dev.mundorf.esportstracker.repository.TeamRepository;
import dev.mundorf.esportstracker.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack happy path: register → login → follow → GET /api/feed returns the followed data.
 * Runs the real Spring context (security, JPA, mappers, controllers) against H2 in Postgres
 * mode, mirroring how a real client would use the API. Complements the unit/slice tests, which
 * mock across layer boundaries and so can't catch integration bugs like a missing @EntityGraph,
 * a security-config misroute, or a JSON shape mismatch.
 *
 * The Riot client is mocked out so a cron-scheduled sync tick (should one fire during the test
 * window) can't reach the real API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// The test is @Transactional so its inserts roll back at teardown - without this, seeded rows
// leak into the shared in-memory H2 DB and break the @DataJpaTest classes that seed the same
// tables. MockMvc runs on the calling thread so the register/login roundtrips share this
// transaction, and the auth flow sees the inserted user without needing an explicit commit.
@Transactional
class FeedFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private MatchRepository matchRepository;

    @MockBean
    private RiotEsportsClient riotEsportsClient;

    @Test
    void shouldRegisterLoginFollowLeagueAndSeeMatchingUpcomingMatchesInFeed() throws Exception {
        Game lol = gameRepository.findBySlug("league-of-legends").orElseThrow();
        League lec = leagueRepository.save(new League("LEC", "lec", "EMEA", lol, "L1"));
        Tournament runningLec = tournamentRepository.save(new Tournament("LEC Split 3 2026", "lec_split_3_2026",
                lec, lol, LocalDate.now(), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1"));
        Team g2 = teamRepository.save(new Team("G2 Esports", "g2-esports", null, lol, "TG2"));
        Team fnc = teamRepository.save(new Team("Fnatic", "fnatic", null, lol, "TFNC"));
        matchRepository.save(new Match(runningLec, lol, g2, fnc,
                Instant.now().plusSeconds(3600), EventStatus.UPCOMING, 0, 0, null, "M1"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"integration-user","email":"it@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"integration-user","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginBody.get("token").asText();
        assertThat(token).isNotBlank();
        String bearer = "Bearer " + token;

        // /api/feed with no follows yet returns empty content but a valid envelope
        mockMvc.perform(get("/api/feed").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingMatches").isEmpty())
                .andExpect(jsonPath("$.runningTournaments").isEmpty());

        // Following just the game must NOT populate the feed — game follows are a UI grouping;
        // only league/team follows drive feed content.
        mockMvc.perform(put("/api/users/me/games")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slugs":["league-of-legends"],"version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followedGames[0].slug").value("league-of-legends"));
        mockMvc.perform(get("/api/feed").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingMatches").isEmpty())
                .andExpect(jsonPath("$.runningTournaments").isEmpty());

        // The public league list is how a client discovers league ids to follow
        mockMvc.perform(get("/api/leagues").param("game", "league-of-legends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("lec"));

        // Each follow-update PUT bumps the version (optimistic locking - see User.version), so the
        // next PUT must submit the current version. Read it via a fresh GET rather than trusting
        // followGamesResult's own body: this test runs the whole flow inside one outer
        // @Transactional (for rollback-after-test), so the PUT's own response can be serialized
        // before Hibernate has actually flushed that PUT's version-incrementing UPDATE - a GET
        // forces a query, which forces the pending flush first, so it always reflects committed state.
        MvcResult meResult = mockMvc.perform(get("/api/users/me").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn();
        long versionAfterGames = objectMapper.readTree(meResult.getResponse().getContentAsString())
                .get("version").asLong();

        mockMvc.perform(put("/api/users/me/leagues")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leagueIds":["%s"],"version":%d}
                                """.formatted(lec.getId(), versionAfterGames)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followedLeagues[0].slug").value("lec"));

        mockMvc.perform(get("/api/feed").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingMatches[0].teamA.name").value("G2 Esports"))
                .andExpect(jsonPath("$.upcomingMatches[0].teamB.name").value("Fnatic"))
                .andExpect(jsonPath("$.upcomingMatches[0].tournamentName").value("LEC Split 3 2026"))
                .andExpect(jsonPath("$.runningTournaments[0].name").value("LEC Split 3 2026"));

        // Auth check: same call without the token is rejected
        mockMvc.perform(get("/api/feed"))
                .andExpect(status().isUnauthorized());
    }
}
