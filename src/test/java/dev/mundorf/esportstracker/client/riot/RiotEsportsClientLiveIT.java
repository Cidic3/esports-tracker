package dev.mundorf.esportstracker.client.riot;

import dev.mundorf.esportstracker.client.riot.dto.RiotLeague;
import dev.mundorf.esportstracker.client.riot.dto.RiotScheduleEvent;
import dev.mundorf.esportstracker.client.riot.dto.RiotStandingEntry;
import dev.mundorf.esportstracker.client.riot.dto.RiotTournament;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hits the real, live Riot LoL Esports API (public key, no registration needed). Named with the
 * "IT" suffix so Maven Surefire's default pattern skips it during a normal `mvn test` run -
 * a test that depends on a third-party service being up shouldn't be able to fail the regular
 * build. Run explicitly with `mvn test -Dtest=RiotEsportsClientLiveIT`.
 */
class RiotEsportsClientLiveIT {

    private static final String API_KEY = "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z";
    private static final String BASE_URL = "https://esports-api.lolesports.com/persisted/gw";

    private final RiotEsportsClient client = new RiotEsportsClient(
            RestClient.builder().baseUrl(BASE_URL).defaultHeader("x-api-key", API_KEY).build());

    @Test
    void shouldFetchRealLeaguesIncludingLec() {
        List<RiotLeague> leagues = client.getLeagues();

        assertThat(leagues).isNotEmpty();
        assertThat(leagues).anyMatch(league -> "lec".equals(league.slug()));
    }

    @Test
    void shouldFetchTournamentsForLec() {
        String lecId = findLeagueId("lec");

        List<RiotTournament> tournaments = client.getTournamentsForLeague(lecId);

        assertThat(tournaments).isNotEmpty();
    }

    @Test
    void shouldFetchScheduleForLec() {
        String lecId = findLeagueId("lec");

        List<RiotScheduleEvent> events = client.getSchedule(lecId);

        assertThat(events).isNotEmpty();
        assertThat(events.get(0).league().slug()).isEqualTo("lec");
    }

    @Test
    void shouldFetchStandingsForAFinishedLecSplit() {
        String lecId = findLeagueId("lec");
        String tournamentId = client.getTournamentsForLeague(lecId).stream()
                .filter(t -> t.slug().contains("split_2_2026") || t.slug().contains("summer_2025"))
                .findFirst()
                .orElseThrow()
                .id();

        List<RiotStandingEntry> entries = client.getStandings(tournamentId);

        assertThat(entries).isNotEmpty();
        assertThat(entries).allSatisfy(entry -> {
            assertThat(entry.groupName()).isNotBlank();
            assertThat(entry.rank()).isPositive();
            assertThat(entry.team().id()).isNotBlank();
        });
    }

    private String findLeagueId(String slug) {
        return client.getLeagues().stream()
                .filter(league -> slug.equals(league.slug()))
                .findFirst()
                .orElseThrow()
                .id();
    }
}
