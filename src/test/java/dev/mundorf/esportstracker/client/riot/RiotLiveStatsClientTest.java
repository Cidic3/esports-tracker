package dev.mundorf.esportstracker.client.riot;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RiotLiveStatsClientTest {

    @ParameterizedTest
    @CsvSource({
            "2026-07-09T17:10:00Z,      2026-07-09T17:10:00Z",
            "2026-07-09T17:10:07Z,      2026-07-09T17:10:00Z",
            "2026-07-09T17:10:19.500Z,  2026-07-09T17:10:10Z",
            "2026-07-09T17:10:59.999Z,  2026-07-09T17:10:50Z",
    })
    void shouldRoundDownToTenSecondBoundary(String input, String expected) {
        assertThat(RiotLiveStatsClient.roundToTenSeconds(Instant.parse(input)))
                .isEqualTo(Instant.parse(expected));
    }
}
