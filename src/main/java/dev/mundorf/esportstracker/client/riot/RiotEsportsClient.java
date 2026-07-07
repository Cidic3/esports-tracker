package dev.mundorf.esportstracker.client.riot;

import dev.mundorf.esportstracker.client.riot.dto.RiotEventDetail;
import dev.mundorf.esportstracker.client.riot.dto.RiotEventTeam;
import dev.mundorf.esportstracker.client.riot.dto.RiotLeague;
import dev.mundorf.esportstracker.client.riot.dto.RiotMatchResult;
import dev.mundorf.esportstracker.client.riot.dto.RiotScheduleEvent;
import dev.mundorf.esportstracker.client.riot.dto.RiotTournament;
import dev.mundorf.esportstracker.exception.ExternalApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class RiotEsportsClient {

    private final RestClient restClient;

    public RiotEsportsClient(RestClient riotRestClient) {
        this.restClient = riotRestClient;
    }

    public List<RiotLeague> getLeagues() {
        try {
            LeaguesEnvelope response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/getLeagues").queryParam("hl", "en-US").build())
                    .retrieve()
                    .body(LeaguesEnvelope.class);
            return response.data().leagues();
        } catch (RestClientException ex) {
            throw new ExternalApiException("Failed to fetch leagues from Riot Esports API", ex);
        }
    }

    public List<RiotTournament> getTournamentsForLeague(String leagueId) {
        try {
            TournamentsEnvelope response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/getTournamentsForLeague")
                            .queryParam("hl", "en-US")
                            .queryParam("leagueId", leagueId)
                            .build())
                    .retrieve()
                    .body(TournamentsEnvelope.class);
            return response.data().leagues().stream()
                    .flatMap(league -> league.tournaments().stream())
                    .toList();
        } catch (RestClientException ex) {
            throw new ExternalApiException("Failed to fetch tournaments for league " + leagueId, ex);
        }
    }

    public List<RiotScheduleEvent> getSchedule(String leagueId) {
        try {
            ScheduleEnvelope response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/getSchedule")
                            .queryParam("hl", "en-US")
                            .queryParam("leagueId", leagueId)
                            .build())
                    .retrieve()
                    .body(ScheduleEnvelope.class);
            return response.data().schedule().events();
        } catch (RestClientException ex) {
            throw new ExternalApiException("Failed to fetch schedule for league " + leagueId, ex);
        }
    }

    public RiotEventDetail getEventDetails(String matchId) {
        try {
            EventDetailsEnvelope response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/getEventDetails")
                            .queryParam("hl", "en-US")
                            .queryParam("id", matchId)
                            .build())
                    .retrieve()
                    .body(EventDetailsEnvelope.class);

            EventDetailsEnvelope.Event event = response.data().event();
            String tournamentId = event.tournament() != null ? event.tournament().id() : null;
            List<RiotEventTeam> teams = event.match().teams().stream()
                    .map(team -> new RiotEventTeam(
                            team.id(),
                            team.name(),
                            team.code(),
                            team.image(),
                            team.result() != null ? team.result().gameWins() : null))
                    .toList();
            return new RiotEventDetail(event.id(), tournamentId, teams);
        } catch (RestClientException ex) {
            throw new ExternalApiException("Failed to fetch event details for match " + matchId, ex);
        }
    }

    // Envelope records mirror Riot's response nesting exactly; kept private since
    // callers only ever need the flat lists returned by the methods above.
    private record LeaguesEnvelope(Data data) {
        private record Data(List<RiotLeague> leagues) {
        }
    }

    private record TournamentsEnvelope(Data data) {
        private record Data(List<LeagueTournaments> leagues) {
        }

        private record LeagueTournaments(List<RiotTournament> tournaments) {
        }
    }

    private record ScheduleEnvelope(Data data) {
        private record Data(Schedule schedule) {
        }

        private record Schedule(List<RiotScheduleEvent> events) {
        }
    }

    private record EventDetailsEnvelope(Data data) {
        private record Data(Event event) {
        }

        private record Event(String id, EventTournament tournament, EventMatch match) {
        }

        private record EventTournament(String id) {
        }

        private record EventMatch(List<EventTeam> teams) {
        }

        private record EventTeam(String id, String name, String code, String image, RiotMatchResult result) {
        }
    }
}
