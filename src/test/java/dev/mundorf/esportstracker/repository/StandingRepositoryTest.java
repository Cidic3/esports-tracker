package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.config.JpaAuditingConfig;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Standing;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Uses the real Flyway migrations against an in-memory H2 (Postgres-compat mode) - see
 * application-test.yml. {@code replace = NONE} prevents Spring Boot's default test-DB
 * auto-replacement so our own H2 URL wins; the seed games row from V9 is present as a result.
 * {@link JpaAuditingConfig} is @Imported because @DataJpaTest doesn't scan @Configuration
 * classes, and without it @CreatedDate / @LastModifiedDate would stay null and the NOT NULL
 * created_at / updated_at columns would reject every save.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class StandingRepositoryTest {

    @Autowired
    private StandingRepository standingRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TournamentRepository tournamentRepository;

    @Test
    void shouldReturnStandingsGroupedByGroupNameThenOrderedByRank() {
        Game lol = gameRepository.findBySlug("league-of-legends").orElseThrow();
        League lec = leagueRepository.save(new League("LEC", "lec", "EMEA", lol, "L1"));
        Tournament tournament = tournamentRepository.save(new Tournament("LEC Split 2 2026", "lec_split_2_2026",
                lec, lol, LocalDate.now().minusDays(30), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1"));
        Team vitality = teamRepository.save(new Team("Team Vitality", "team-vitality", null, lol, "TVIT"));
        Team g2 = teamRepository.save(new Team("G2 Esports", "g2-esports", null, lol, "TG2"));
        Team fnatic = teamRepository.save(new Team("Fnatic", "fnatic", null, lol, "TFNC"));

        // Insert deliberately out of order (rank 2 first, then rank 1, then a row from a
        // different group) - the repository is what should be sorting the result.
        standingRepository.save(new Standing(tournament, g2, "Regular Season", 2, 7, 2));
        standingRepository.save(new Standing(tournament, vitality, "Regular Season", 1, 8, 1));
        standingRepository.save(new Standing(tournament, fnatic, "Play-Ins", 1, 3, 0));

        List<Standing> result = standingRepository.findByTournamentIdOrderByGroupNameAscRankAsc(tournament.getId());

        assertThat(result)
                .extracting(Standing::getGroupName, Standing::getRank, s -> s.getTeam().getSlug())
                .containsExactly(
                        tuple("Play-Ins", 1, "fnatic"),
                        tuple("Regular Season", 1, "team-vitality"),
                        tuple("Regular Season", 2, "g2-esports"));
    }
}
