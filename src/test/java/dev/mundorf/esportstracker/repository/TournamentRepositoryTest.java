package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.config.JpaAuditingConfig;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class TournamentRepositoryTest {

    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private MatchRepository matchRepository;

    @Test
    void shouldFindRunningTournamentsMatchingFollowedGameOrFollowedTeamViaAnyOfItsMatches() {
        Game lol = gameRepository.findBySlug("league-of-legends").orElseThrow();
        Game dota = gameRepository.findBySlug("dota-2").orElseThrow();
        League lec = leagueRepository.save(new League("LEC", "lec", "EMEA", lol, "L1"));
        League dpc = leagueRepository.save(new League("DPC", "dpc", "GLOBAL", dota, "L2"));
        Tournament runningLec = tournamentRepository.save(new Tournament("LEC Split 3 2026", "lec_split_3_2026",
                lec, lol, LocalDate.now(), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1"));
        Tournament runningDpcWithFollowedTeam = tournamentRepository.save(new Tournament("DPC 2026", "dpc_2026",
                dpc, dota, LocalDate.now(), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T2"));
        Tournament runningDpcWithoutFollows = tournamentRepository.save(new Tournament("Other DPC 2026",
                "dpc_other_2026", dpc, dota, LocalDate.now(), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T3"));
        Tournament finishedLec = tournamentRepository.save(new Tournament("LEC Split 2 2026", "lec_split_2_2026",
                lec, lol, LocalDate.now().minusDays(60), LocalDate.now().minusDays(10),
                TournamentTier.PRIMARY, EventStatus.FINISHED, null, "T4"));

        Team teamLiquid = teamRepository.save(new Team("Team Liquid", "team-liquid", null, dota, "TTL"));
        Team og = teamRepository.save(new Team("OG", "og", null, dota, "TOG"));
        Team other = teamRepository.save(new Team("Other", "other", null, dota, "TOTHER"));
        // Team Liquid plays one match in runningDpcWithFollowedTeam - that's the link that
        // should pull that tournament into the result via the followed-team clause.
        matchRepository.save(new Match(runningDpcWithFollowedTeam, dota, teamLiquid, og,
                Instant.now().plusSeconds(3600), EventStatus.UPCOMING, 0, 0, null, "M1"));
        // runningDpcWithoutFollows has a match but with two non-followed teams - proves the
        // EXISTS subquery correctly narrows to only tournaments a followed team actually plays in.
        matchRepository.save(new Match(runningDpcWithoutFollows, dota, og, other,
                Instant.now().plusSeconds(3600), EventStatus.UPCOMING, 0, 0, null, "M2"));

        // User follows: game=LoL and team=Team Liquid.
        List<Tournament> result = tournamentRepository.findRunningForFollowed(
                Set.of(lol.getId()), Set.of(teamLiquid.getId()), PageRequest.of(0, 20));

        assertThat(result)
                .extracting(Tournament::getExternalId)
                .containsExactlyInAnyOrder(runningLec.getExternalId(), runningDpcWithFollowedTeam.getExternalId())
                .doesNotContain(runningDpcWithoutFollows.getExternalId(), finishedLec.getExternalId());
    }
}
