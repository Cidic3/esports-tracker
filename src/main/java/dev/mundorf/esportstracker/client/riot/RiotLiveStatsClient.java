package dev.mundorf.esportstracker.client.riot;

import dev.mundorf.esportstracker.client.riot.dto.RiotGameWindow;
import dev.mundorf.esportstracker.client.riot.dto.RiotParticipantStats;
import dev.mundorf.esportstracker.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Client for feed.lolesports.com/livestats — Riot's per-game stats feed (champions,
 * items, objectives). Unlike the persisted/gw API it needs no API key.
 *
 * <p>Quirks discovered empirically (nothing here is officially documented):
 * <ul>
 *   <li>{@code startingTime} must be a multiple of 10 seconds; it selects which frames
 *       of the game timeline are returned.</li>
 *   <li>Omitting it returns frames from the very start of the game (all stats zero) —
 *       useful only for reading the first frame's timestamp and the participant roster.</li>
 *   <li>A startingTime more than ~1 hour past the game's final frame is rejected with
 *       400, so finding a finished game's end state requires walking forward from the
 *       start (see MatchDetailsService).</li>
 *   <li>Games with no stats coverage return an empty/204 body.</li>
 * </ul>
 */
@Component
public class RiotLiveStatsClient {

    private final RestClient restClient;

    public RiotLiveStatsClient(@Qualifier("riotFeedRestClient") RestClient riotFeedRestClient) {
        this.restClient = riotFeedRestClient;
    }

    /**
     * Roster + team objective totals at the given time; null when the game has no stats
     * coverage, or when startingTime overshoots what the feed will serve (400).
     */
    public RiotGameWindow getWindow(String gameId, Instant startingTime) {
        WindowEnvelope response = fetch("/window/" + gameId, startingTime, WindowEnvelope.class);
        if (response == null || response.frames() == null || response.frames().isEmpty()) {
            return null;
        }

        WindowEnvelope.Frame lastFrame = response.frames().getLast();
        return new RiotGameWindow(
                Instant.parse(response.frames().getFirst().rfc460Timestamp()),
                lastFrame.gameState(),
                toTeamFrame(response.gameMetadata().blueTeamMetadata(), lastFrame.blueTeam()),
                toTeamFrame(response.gameMetadata().redTeamMetadata(), lastFrame.redTeam()));
    }

    /** Per-player stat lines at the given time; null under the same conditions as getWindow. */
    public List<RiotParticipantStats> getDetails(String gameId, Instant startingTime) {
        DetailsEnvelope response = fetch("/details/" + gameId, startingTime, DetailsEnvelope.class);
        if (response == null || response.frames() == null || response.frames().isEmpty()) {
            return null;
        }
        return response.frames().getLast().participants().stream()
                .map(p -> new RiotParticipantStats(
                        p.participantId(), p.level(), p.kills(), p.deaths(), p.assists(),
                        p.creepScore(), p.totalGoldEarned(),
                        p.items() == null ? List.of() : p.items()))
                .toList();
    }

    private <T> T fetch(String path, Instant startingTime, Class<T> type) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        if (startingTime != null) {
                            uriBuilder.queryParam("startingTime", roundToTenSeconds(startingTime));
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(type);
        } catch (HttpClientErrorException.BadRequest ex) {
            // startingTime out of the feed's serviceable range — caller treats as "no data here"
            return null;
        } catch (RestClientException ex) {
            throw new ExternalApiException("Failed to fetch live stats " + path, ex);
        }
    }

    /** The feed rejects timestamps that aren't on a 10-second boundary. */
    static Instant roundToTenSeconds(Instant instant) {
        Instant truncated = instant.truncatedTo(ChronoUnit.SECONDS);
        return truncated.minusSeconds(truncated.getEpochSecond() % 10);
    }

    private record WindowEnvelope(GameMetadata gameMetadata, List<Frame> frames) {
        private record GameMetadata(TeamMetadata blueTeamMetadata, TeamMetadata redTeamMetadata) {
        }

        private record TeamMetadata(String esportsTeamId, List<ParticipantMetadata> participantMetadata) {
        }

        private record ParticipantMetadata(int participantId, String summonerName, String championId,
                                           String role) {
        }

        private record Frame(String rfc460Timestamp, String gameState, TeamFrame blueTeam, TeamFrame redTeam) {
        }

        // dragons are plain strings in the payload: "ocean", "infernal", ...
        private record TeamFrame(int totalGold, int inhibitors, int towers, int barons, int totalKills,
                                 List<String> dragons) {
        }
    }

    private static RiotGameWindow.RiotGameTeamFrame toTeamFrame(
            WindowEnvelope.TeamMetadata metadata, WindowEnvelope.TeamFrame frame) {
        List<RiotGameWindow.RiotParticipantMeta> participants = metadata.participantMetadata().stream()
                .map(p -> new RiotGameWindow.RiotParticipantMeta(
                        p.participantId(), p.summonerName(), p.championId(), p.role()))
                .toList();
        return new RiotGameWindow.RiotGameTeamFrame(
                participants,
                frame.totalGold(), frame.towers(), frame.barons(), frame.totalKills(),
                frame.dragons() == null ? List.of() : frame.dragons());
    }

    private record DetailsEnvelope(List<Frame> frames) {
        private record Frame(List<Participant> participants) {
        }

        private record Participant(int participantId, int level, int kills, int deaths, int assists,
                                   int creepScore, long totalGoldEarned, List<Integer> items) {
        }
    }
}
