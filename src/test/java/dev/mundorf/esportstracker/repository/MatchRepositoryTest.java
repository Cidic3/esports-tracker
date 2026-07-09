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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused on {@link MatchRepository#findUpcomingForFollowed} - the OR-semantics query with the
 * "callers must not pass an empty set" caveat and the placeholder-UUID trick documented on the
 * method. Trivial derived queries (findByGameIdAndExternalId etc.) aren't tested here since a
 * failure would show up immediately in any other test that uses them.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class MatchRepositoryTest {

    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TournamentRepository tournamentRepository;

    @Test
    void shouldFindUpcomingMatchesMatchingFollowedLeagueOrEitherFollowedTeam() {
        Game lol = gameRepository.findBySlug("league-of-legends").orElseThrow();
        Game dota = gameRepository.findBySlug("dota-2").orElseThrow();
        League lec = leagueRepository.save(new League("LEC", "lec", "EMEA", lol, "L1"));
        League dpc = leagueRepository.save(new League("DPC", "dpc", "GLOBAL", dota, "L2"));
        Tournament lecTourney = tournamentRepository.save(new Tournament("LEC Split 3 2026", "lec_split_3_2026",
                lec, lol, LocalDate.now(), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T1"));
        Tournament dpcTourney = tournamentRepository.save(new Tournament("DPC 2026", "dpc_2026",
                dpc, dota, LocalDate.now(), LocalDate.now().plusDays(30),
                TournamentTier.PRIMARY, EventStatus.RUNNING, null, "T2"));

        Team g2 = teamRepository.save(new Team("G2 Esports", "g2-esports", null, lol, "TG2"));
        Team fnc = teamRepository.save(new Team("Fnatic", "fnatic", null, lol, "TFNC"));
        Team teamLiquid = teamRepository.save(new Team("Team Liquid", "team-liquid", null, dota, "TTL"));
        Team og = teamRepository.save(new Team("OG", "og", null, dota, "TOG"));
        Team peakInternal = teamRepository.save(new Team("Peak Internal", "peak-internal", null, dota, "TPI"));

        Instant tomorrow = Instant.now().plusSeconds(86_400);
        Match qualifiesByLeague = matchRepository.save(new Match(lecTourney, lol, g2, fnc,
                tomorrow, EventStatus.UPCOMING, 0, 0, null, "M1"));
        Match qualifiesByTeam = matchRepository.save(new Match(dpcTourney, dota, teamLiquid, og,
                tomorrow.plusSeconds(3600), EventStatus.UPCOMING, 0, 0, null, "M2"));
        Match excludedByStatus = matchRepository.save(new Match(dpcTourney, dota, teamLiquid, og,
                tomorrow, EventStatus.FINISHED, 3, 1, null, "M3"));
        // DPC match with two non-followed teams - the followed-league (LEC) and followed-team
        // (teamLiquid) clauses should both miss, so this must NOT appear in the result.
        Match excludedByLeagueAndTeam = matchRepository.save(new Match(dpcTourney, dota, og, peakInternal,
                tomorrow, EventStatus.UPCOMING, 0, 0, null, "M4"));
        // User follows: league=LEC (so qualifiesByLeague matches) and team=Team Liquid (so
        // qualifiesByTeam matches). qualifiesByTeam is a DPC match, so it only qualifies via
        // the followed team, not the followed league - proves OR semantics work.
        Set<UUID> followedLeagues = Set.of(lec.getId());
        Set<UUID> followedTeams = Set.of(teamLiquid.getId());

        Page<Match> result = matchRepository.findUpcomingForFollowed(followedLeagues, followedTeams,
                PageRequest.of(0, 20, Sort.by("scheduledAt").ascending()));

        assertThat(result.getContent())
                .extracting(Match::getExternalId)
                .containsExactly(qualifiesByLeague.getExternalId(), qualifiesByTeam.getExternalId())
                .doesNotContain(excludedByStatus.getExternalId(), excludedByLeagueAndTeam.getExternalId());
    }

    @Test
    void shouldReturnEmptyPageWhenBothFollowSetsContainOnlyThePlaceholderUuid() {
        // Simulates the "user follows nothing" service-layer behavior: the service substitutes a
        // random non-matching UUID rather than passing an empty set (empty IN () is not portable).
        // The query should then return nothing - proving the placeholder trick is safe.
        Set<UUID> placeholder = Set.of(new UUID(0, 0));

        Page<Match> result = matchRepository.findUpcomingForFollowed(placeholder, placeholder,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }
}
