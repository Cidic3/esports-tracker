package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Standing;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.repository.StandingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandingServiceTest {

    @Mock
    private StandingRepository standingRepository;

    @InjectMocks
    private StandingService standingService;

    @Test
    void shouldReturnStandingsForTournamentOrderedByGroupAndRank() {
        UUID tournamentId = UUID.randomUUID();
        Game lolGame = new Game("League of Legends", "league-of-legends", null);
        League lec = new League("LEC", "lec", "EMEA", lolGame, "L1");
        Tournament tournament = new Tournament("LEC Split 2 2026", "lec_split_2_2026", lec, lolGame,
                LocalDate.now().minusDays(30), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1");
        Team team = new Team("G2 Esports", "g2-esports", null, lolGame, "TA");
        Standing standing = new Standing(tournament, team, "Regular Season", 1, 8, 1);
        when(standingRepository.findByTournamentIdOrderByGroupNameAscRankAsc(tournamentId))
                .thenReturn(List.of(standing));

        List<Standing> result = standingService.findByTournament(tournamentId);

        assertThat(result).containsExactly(standing);
    }
}
