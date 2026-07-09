package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.client.riot.RiotEsportsClient;
import dev.mundorf.esportstracker.client.riot.RiotLiveStatsClient;
import dev.mundorf.esportstracker.client.riot.dto.RiotEventGame;
import dev.mundorf.esportstracker.client.riot.dto.RiotGameWindow;
import dev.mundorf.esportstracker.client.riot.dto.RiotParticipantStats;
import dev.mundorf.esportstracker.model.dto.MatchDetailsResponse;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchDetailsServiceTest {

    @Mock
    private MatchService matchService;

    @Mock
    private RiotEsportsClient esportsClient;

    @Mock
    private RiotLiveStatsClient liveStatsClient;

    @InjectMocks
    private MatchDetailsService matchDetailsService;

    private static final Instant GAME_START = Instant.parse("2026-07-09T16:00:00Z");
    private static final UUID MATCH_ID = UUID.randomUUID();

    private Match matchFor(String gameSlug) {
        Game game = new Game(gameSlug, gameSlug, null);
        League league = new League("LEC", "lec", "EMEA", game, "L1");
        Tournament tournament = new Tournament("LEC Split 2", "lec_split_2", league, game,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(10),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1");
        Team teamA = new Team("EWI", "ewi", null, game, "TA");
        Team teamB = new Team("Spandau", "spandau", null, game, "TB");
        return new Match(tournament, game, teamA, teamB, GAME_START,
                EventStatus.FINISHED, 1, 0, null, "MATCH-EXT");
    }

    private static RiotGameWindow window(String gameState) {
        var blueMeta = new RiotGameWindow.RiotParticipantMeta(1, "EWI Vizicsacsi", "Rumble", "top");
        var redMeta = new RiotGameWindow.RiotParticipantMeta(6, "SPD Player", "Ahri", "mid");
        return new RiotGameWindow(
                GAME_START,
                gameState,
                new RiotGameWindow.RiotGameTeamFrame(List.of(blueMeta), 58000, 9, 2, 25, List.of("ocean")),
                new RiotGameWindow.RiotGameTeamFrame(List.of(redMeta), 48000, 3, 0, 13, List.of()));
    }

    @Test
    void shouldReturnEmptyGamesForNonLolMatch() {
        when(matchService.findById(MATCH_ID)).thenReturn(matchFor("dota-2"));

        MatchDetailsResponse response = matchDetailsService.getDetails(MATCH_ID);

        assertThat(response.games()).isEmpty();
        verifyNoInteractions(esportsClient, liveStatsClient);
    }

    @Test
    void shouldMergeWindowRosterWithDetailStatsForCompletedGame() {
        when(matchService.findById(MATCH_ID)).thenReturn(matchFor("league-of-legends"));
        when(esportsClient.getEventGames("MATCH-EXT"))
                .thenReturn(List.of(new RiotEventGame("G1", 1, "completed", "TA", "TB")));
        when(liveStatsClient.getWindow(eq("G1"), isNull())).thenReturn(window("in_game"));
        // first walk probe already sees the finished frame
        when(liveStatsClient.getWindow(eq("G1"), eq(GAME_START.plusSeconds(20 * 60))))
                .thenReturn(window("finished"));
        when(liveStatsClient.getDetails(eq("G1"), eq(GAME_START.plusSeconds(20 * 60))))
                .thenReturn(List.of(new RiotParticipantStats(1, 16, 8, 2, 7, 198, 12483L, List.of(3009, 6653))));

        MatchDetailsResponse response = matchDetailsService.getDetails(MATCH_ID);

        assertThat(response.games()).hasSize(1);
        var game = response.games().getFirst();
        assertThat(game.state()).isEqualTo("completed");
        assertThat(game.blueTeam().name()).isEqualTo("EWI");
        assertThat(game.redTeam().name()).isEqualTo("Spandau");
        assertThat(game.blueTeam().totalKills()).isEqualTo(25);
        var player = game.blueTeam().players().getFirst();
        assertThat(player.champion()).isEqualTo("Rumble");
        assertThat(player.kills()).isEqualTo(8);
        assertThat(player.items()).containsExactly(3009, 6653);
        // red-side player had no stat line -> roster info kept, stats zeroed
        assertThat(game.redTeam().players().getFirst().champion()).isEqualTo("Ahri");
        assertThat(game.redTeam().players().getFirst().kills()).isZero();
    }

    @Test
    void shouldWalkForwardUntilFinishedFrame() {
        when(matchService.findById(MATCH_ID)).thenReturn(matchFor("league-of-legends"));
        when(esportsClient.getEventGames("MATCH-EXT"))
                .thenReturn(List.of(new RiotEventGame("G1", 1, "completed", "TA", "TB")));
        when(liveStatsClient.getWindow(eq("G1"), isNull())).thenReturn(window("in_game"));
        // 20-minute probe still mid-game; 30-minute probe sees the end
        when(liveStatsClient.getWindow(eq("G1"), eq(GAME_START.plusSeconds(20 * 60))))
                .thenReturn(window("in_game"));
        when(liveStatsClient.getWindow(eq("G1"), eq(GAME_START.plusSeconds(30 * 60))))
                .thenReturn(window("finished"));
        when(liveStatsClient.getDetails(eq("G1"), any())).thenReturn(null);

        matchDetailsService.getDetails(MATCH_ID);

        verify(liveStatsClient).getDetails("G1", GAME_START.plusSeconds(30 * 60));
    }

    @Test
    void shouldDegradeToGameWithoutStatsWhenFeedHasNoCoverage() {
        when(matchService.findById(MATCH_ID)).thenReturn(matchFor("league-of-legends"));
        when(esportsClient.getEventGames("MATCH-EXT"))
                .thenReturn(List.of(new RiotEventGame("G1", 1, "completed", "TA", "TB")));
        when(liveStatsClient.getWindow(eq("G1"), isNull())).thenReturn(null);

        MatchDetailsResponse response = matchDetailsService.getDetails(MATCH_ID);

        assertThat(response.games()).hasSize(1);
        assertThat(response.games().getFirst().blueTeam()).isNull();
        assertThat(response.games().getFirst().redTeam()).isNull();
    }

    @Test
    void shouldNotFetchStatsForUnstartedGames() {
        when(matchService.findById(MATCH_ID)).thenReturn(matchFor("league-of-legends"));
        when(esportsClient.getEventGames("MATCH-EXT"))
                .thenReturn(List.of(new RiotEventGame("G3", 3, "unstarted", "TA", "TB")));

        MatchDetailsResponse response = matchDetailsService.getDetails(MATCH_ID);

        assertThat(response.games()).hasSize(1);
        assertThat(response.games().getFirst().state()).isEqualTo("unstarted");
        verifyNoInteractions(liveStatsClient);
    }
}
