package dev.mundorf.esportstracker.client.cito;

import dev.mundorf.esportstracker.client.cito.dto.CitoAlgsEvent;
import dev.mundorf.esportstracker.exception.ExternalApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Client for the Cito API's ALGS endpoints (Apex Legends esports data, aggregated from official
 * ALGS results - EA/Respawn publish no API of their own, see CLAUDE.md). The free tier is capped
 * at 500 calls/month, which is why sync makes exactly ONE call per cycle: the events list already
 * embeds full statsData for completed days, so schedules, results and standings all arrive
 * together - never call a per-event endpoint from sync.
 */
@Component
public class CitoClient {

    private final RestClient restClient;

    public CitoClient(RestClient citoRestClient) {
        this.restClient = citoRestClient;
    }

    /** The latest ALGS match days (Cito returns 50 per call), newest season first. */
    public List<CitoAlgsEvent> getAlgsEvents() {
        try {
            EventsEnvelope response = restClient.get()
                    .uri("/apex/algs/events")
                    .retrieve()
                    .body(EventsEnvelope.class);
            return response.data();
        } catch (RestClientException ex) {
            throw new ExternalApiException("Failed to fetch ALGS events from Cito API", ex);
        }
    }

    // Envelope mirrors Cito's {success, count, data} wrapper; callers only need the list.
    private record EventsEnvelope(boolean success, int count, List<CitoAlgsEvent> data) {
    }
}
